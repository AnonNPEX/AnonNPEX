# Spring Hateoas
This project provides some APIs to ease creating REST representations that follow the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) principle when working with Spring and especially Spring MVC. The core problem it tries to address is link creation and representation assembly.

## JAXB / JSON integration
As representations for REST web services are usually rendered in either XML or JSON the natural choice of technology to achieve this is either JAXB, JSON or both in combination. To follow HATEOAS principles you need to incorporate links into those representation. Spring HATEOAS provides a set of useful types to ease working with those.

## Links
The `Link` value object follows the Atom link definition and consists of a `rel` and an `href` attribute. It contains a few constants for well known rels such as `self`, `next` etc. The XML representation will render in the Atom namespace.

```java
Link link = new Link("http://localhost:8080/something");
assertThat(link.getHref(), is("http://localhost:8080/something"));
assertThat(link.getRel(), is(Link.SELF));

Link link = new Link("http://localhost:8080/something", "my-rel");
assertThat(link.getHref(), is("http://localhost:8080/something"));
assertThat(link.getRel(), is("my-rel"));
```

## Resources
As pretty much every representation of a resource will contain some links (at least the `self` one) we provide a base class to actually inherit from when designing representation classes.
```java
class PersonResource extends ResourceSupport {

  String firstname;
  String lastname;
}
```

Inheriting from `ResourceSupport` will allow adding links easily:

```java
PersonResource resource = new PersonResource();
resource.firstname = "Dave";
resource.lastname = "Matthews";
resource.add(new Link("http://myhost/people"));
```

This would render as follows in JSON:
```java
{ firstname : "Dave",
  lastname : "Matthews",
  links : [ { rel : "self", href : "http://myhost/people" } ] }
```

… or slightly more verbose in XML …
```xml
<person xmlns:atom="http://www.w3.org/2005/Atom">
  <firstname>Dave</firstname>
  <lastname>Matthews</lastname>
  <links>
    <atom:link rel="self" href="http://myhost/people" />
  </links>
</person>
```

You can also easily access links contained in that resource:
```java
Link selfLink = new Link("http://myhost/people");
assertThat(resource.getId(), is(selfLink);
assertThat(resource.getLink(Link.SELF), is(selfLink));
```

## Link builder
Now we've got the domain vocabulary in place, but the main challenge remains: how to create the actual URIs to be wrapped into `Link`s in a less fragile way. Right now we'd have to duplicate URI strings all over the place which is brittle and unmaintainable. 

Assume you have your Spring MVC controllers implemented as follows:

```java
@Controller
@RequestMapping("/people")
class PersonController {

  @RequestMapping(method = RequestMethod.GET)
  public HttpEntity<PersonResource> showAll() { … }

  @RequestMapping(value = "/{person}", method = RequestMethod.GET)
  public HttpEntity<PersonResource> show(@PathVariable Long person) { … }
}
```

We see two conventions here. There's a collection resource exposed through the controller class' `@RequestMapping` annotation with individual elements of that collections exposed as direct sub resource. The collection resource might be exposed at a simple URI (as just shown) or more complex ones like `/people/{id}/addresses`.
Let's say you would like to actually link to the collection resource of all people. Following the approach from up above would cause two problems:

1. To create an absolute URI you'd need to lookup the protocol, hostname, port, servlet base etc. This is cumbersome and requires ugly manual string concatenation code.
2. You probably don't want to concatenate the `/people` on top of your base URI because you'd have to maintain the information in multiple places then. Change the mapping, change all the clients pointing to it.

Spring Hateoas now provides a `ControllerLinkBuilder` that allows to create links by pointing to controller classes:

```java
import static org.sfw.hateoas.mvc.ControllerLinkBuilder.*;

Link link = linkTo(PersonController.class).withRel("people");
assertThat(link.getRel(), is("people"));
assertThat(link.getHref(), endsWith("/people"));
```

The `ControllerLinkBuilder` uses Spring's `ServletUriComponentsBuilder` under the hood to obtain the basic URI information from the current request. Assuming your application runs at `http://localhost:8080/your-app` This will be exactly the URI you're constructing additional parts on top. The builder now inspects the given controller class for its root mapping and thus end up with `http://localhost:8080/your-app/people`. You can also easily build more nested links as well:

```java
Person person = new Person(1L, "Dave", "Matthews");
//                 /person                 /     1
Link link = linkTo(PersonController.class).slash(person.getId()).withSelfRel();
assertThat(link.getRel(), is(Link.SELF));
assertThat(link.getHref(), endsWith("/people/1"));
```

If your domain class implements the `Identifiable` interface the `slash(…)` method will rather invoke `getId()` on the given object instead of `toString()`. Thus the just shown link creation can be abbreviated to:

```java
class Person implements Identifiable<Long> {
  public Long getId() { … }
}

Link link = linkTo(PersonController.class).slash(person).withSelfRel();
```

The builder also allows creating URI instances to build up e.g. response header values:

```java
HttpHeaders headers = new HttpHeaders();
headers.setLocation(linkTo(PersonController.class).slash(person).toUri());
return new ResponseEntity<PersonResource>(headers, HttpStatus.CREATED);
```

### Building links pointing to methods

As of version 0.4 you can even easily build links pointing to methods or creating dummy controller method invocations. The first approach is to hand a `Method` instance to the `ControllerLinkBuilder`:

```java
Method method = PersonController.class.getMethod("show", Long.class);
Link link = linkTo(method, 2L).withSelfRel();

assertThat(link.getHref(), is("/people/2")));
```

This is still a bit dissatisfying as we have to get a `Method` instance first, which throws an exception and is generally quite cumbersome. At least we don't repeat the mapping. An even better approach is to have a dummy method invocation of the target method on a controller proxy we can create easily using the `methodOn(…)` helper.

```java
Link link = linkTo(methodOn(PersonController.class).show(2L)).withSelfRel();
assertThat(link.getHref(), is("/people/2")));
```

`methodOn(…)` creates a proxy of the controller class that is recording the method invocation and exposed it in a proxy created for the return type of the method. This allows the fluent expression of the method we want to obtain the mapping for. However there are a few constraints on the methods that can be obtained using this technique:

1. The return type has to be capable of proxying as we need to expose the method invocation on it.
2. The parameters handed into the methods are generally neglected, except the ones referred to through `@PathVariable` as they make up the URI.

## EntityLinks

So far we have created links by pointing to the web-framework implementations (i.e. Spring MVC controllers or JAX-RS resource classes) and inspected the mapping. In many cases these classes essentially read and write representations backed by a model class.

The `EntityLinks` interfaces now exposes API to lookup `Link`s or `LinkBuilder`s based on the model types. The methods essentially return links to either point to the collection resource (e.g. `/people`) or a single resource (e.g. `/people/1`).

```
EntityLinks links = …;
LinkBuilder builder = links.linkFor(CustomerResource.class);
Link link = links.linkToSingleResource(CustomerResource.class, 1L);
```

`EntityLinks` is available for dependency injection by activating `@EnableEntityLinks` in your Spring MVC configuration. Activating this functionality will cause all your Spring MVC controllers and JAX-RS resource implementations available in the current `ApplicationContext` being inspected for the `@ExposesResourceFor(…)` annotation. The annotation exposes which model type the controller manages. Beyond that we assume you follow the URI mapping convention of a class level base mapping and assuming you have controller methods handling an appended `/{id}`. Here's an example implementation of an `EntityLinks` capable controller:

```java
@Controller
@ExposesResourceFor(Order.class)
@RequestMapping("/orders")
class OrderController {

  @RequestMapping
  ResponseEntity orders(…) { … }

  @RequestMapping("/{id}")
  ResponseEntity order(@PathVariable("id") … ) { … }
}
```

The controller exposes that it manages `Order` instances and exposes handler methods that are mapped to our convention. Enabling `EntityLinks` through `@EnableEntityLinks` in your Spring MVC configuration you can now go ahead and create links to the just shown controller as follows.

```java
@Controller
class PaymentController {

  @Autowired EntityLinks entityLinks;

  @RequestMapping(…, method = HttpMethod.PUT)
  ResponseEntity payment(@PathVariable Long orderId) {

	Link link = entityLinks.linkToSingleResource(Order.class, orderId);
    …
  }
}
```

As you can see you can refer to the link `Order` instances are handled at without even referring to the `OrderController`.

## Resource assembler

As the mapping from an entity to a resource type will have to be used in multiple places it makes sense to create a dedicated class responsible for doing so. The conversion will of course contain very custom steps but also a few boilerplate ones:

1. Instantiation of the resource class
2. Adding a link with rel `self` pointing to the resource that gets rendered.

Spring Hateoas now provides a `ResourceAssemblerSupport` base class that helps reducing the amount of code needed to be written:

```java
class PersonResourceAssembler extends ResourceAssemblerSupport<Person, PersonResource> {

  public PersonResourceAssembler() {
    super(PersonController.class, PersonResource.class);
  }

  @Override
  public PersonResource toResource(Person person) {

    PersonResource resource = createResource(person);
    // … do further mapping
    return resource;
  }
}
```

Setting the class up like this gives you the following benefits: there are a hand full of `createResource(…)` methods that will allow you to create an instance of the resource and have it a `Link` with a rel of `self` added to it. The href of that link is determined by the configured controllers request mapping plus the id of the `Identifiable` (e.g. `/people/1` in our case). The resource type gets instantiated by reflection and expects a no-arg constructor. Simply override `instantiateResource(…)` in case you'd like to use a dedicated constructor or avoid the reflection performance overhead.

The assembler can then be used to either assemble a single resource or an `Iterable` of them:

```java
Person person = new Person(…);
Iterable<Person> people = Collections.singletonList(person);

PersonResourceAssembler assembler = new PersonResourceAssembler();
PersonResource resource = assembler.toResource(person);
List<PersonResource> resources = assembler.toResource(people);
```

## @EnableHypermediaSupport
To enable the `ResourceSupport` subtypes be rendered according to the specification of various hypermedia representations types, the support for a particular hypermedia representation format can be activated through `@EnableHypermediaSupport`. The annotation takes a `HypermediaType` enumeration as argument. Currently we support [HAL](http://tools.ietf.org/html/draft-kelly-json-hal) as well as a default rendering. Using the annotation triggers the following:

* registers necessary Jackson modules to render `Resource`/`Resources` in the hypermedia specific format.
* if JSONPath is on the classpath, it automatically registers a `LinkDiscoverer` instance to lookup links by their `rel`s in plain JSON representations (see [below](#linkdiscoverer-api)).
* enables `@EnableEntityLinks` by default (see [above](#entitylinks)), will automatically pick up `EntityLinks` implementations and bundle them into a `DelegatingEntityLinks` instance available for autowiring.
* automatically picks up all `RelProvider` implementations in the `ApplicationContext` and bundles them into a `DelegatingRelProvider` available for autowiring. Registers providers to consider `@Relation` on domain types as well as Spring MVC controllers. If [EVO inflector](https://github.com/atteo/evo-inflector) is on the classpath collection rels are derived using the pluralizing algorithm implemented in the library (see [below](#relprovider-api)).

## LinkDiscoverer API
When working with hypermedia enabled representations, a common task is to find a link with a particular relation type in them. Spring HATEOAS provides [JSONPath](https://code.google.com/p/json-path/) based implementations of the `LinkProvider` interface for either the default representation rendering or HAL out of the box. When using `@EnableHypermediaSupport` we automatically expose an instance supporting the configured hypermedia type as Spring bean.

Alternatively you can simply setup and use an instance like this:

```java
String content = "{'_links' :  { 'foo' : { 'href' : '/foo/bar' }}}";
LinkDiscoverer discoverer = new HalLinkDiscoverer();
Link link = discoverer.findLinkWithRel("foo", content);

assertThat(link.getRel(), is("foo"));
assertThat(link.getHref(), is("/foo/bar"));
```

## RelProvider API
When building links you usually need to determine the relation type to be used for the like. In most cases the relation type is directly associated with a (domain) type. We encapsulate the detailed algorithm to lookup the relation types behind a `RelProvider` API that allows to determine the relation types for single and collection resources. Here's the algorithm the relation type is looked up:

1. If the type is annotated with `@Relation` we use the values configured in the annotation.
1. if not, we default to the uncapitalized simple class name plus an appended `List` for the collection rel.
1. in case the [EVO inflector](https://github.com/atteo/evo-inflector) JAR is in the classpath, we rather use the plural of the single resource rel provided by the pluralizing algorithm.
1. `@Controller` classes annotated with `@ExposesResourceFor` (see section on [EntityLinks](#entitylinks) for details) will transparently lookup the relation types for the type configured in the annotation, so that you can use `relProvider.getSingleResourceRelFor(MyController.class)` and get the relation type of the domain type exposed.

A `RelProvider` is exposed as Spring bean when using `@EnableHypermediaSupport` automatically. You can plug in custom providers by simply implementing the interface and exposing them as Spring bean in turn.

## Traverson

As of version 0.11 Spring HATEOAS provides an API for client side service traversal inspired by the [Traverson](https://blog.codecentric.de/en/2013/11/traverson/) JavaScript library.


```java
Map<String, Object> parameters = new HashMap<>();
parameters.put("user", 27);

Traverson traverson = new Traverson("http://localhost:8080/api/", MediaTypes.HAL_JSON);
String name = traverson.follow("movies", "movie", "actor").
  withTemplateParameters(parameters).
  toObject("$.name");
```

You set up a `Traverson` instance by pointing it to a REST server and configure the media types you want to set as `Accept` header. You then go ahead and define the relation names you want to discover and follow. relation names can either be simple names or JSONPath expressions (starting with an `$`).

The sample then hands a parameter map into the execution. The parameters will be used to expand URIs found during the traversal that are templated. The traversal is concluded by accessing the representation of the final traversal. In the case of the sample we evaluate a JSONPath expression to access the actor's name.