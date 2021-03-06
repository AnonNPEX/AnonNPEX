/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executors;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jo??o Andr?? Martins
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.trace.enabled", matchIfMissing = true)
@ConditionalOnClass(PubSubTemplate.class)
@EnableConfigurationProperties(GcpPubSubProperties.class)
public class GcpPubSubAutoConfiguration {

	public static final String DEFAULT_SOURCE_NAME = "spring";

	private final GcpPubSubProperties gcpPubSubProperties;

	private final GcpProjectIdProvider finalProjectIdProvider;

	private final CredentialsProvider finalCredentialsProvider;

	public GcpPubSubAutoConfiguration(GcpPubSubProperties gcpPubSubProperties,
			GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.gcpPubSubProperties = gcpPubSubProperties;
		this.finalProjectIdProvider = gcpPubSubProperties.getProjectId() != null
				? gcpPubSubProperties::getProjectId
				: gcpProjectIdProvider;
		this.finalCredentialsProvider = gcpPubSubProperties.getCredentials().getLocation() != null
				? FixedCredentialsProvider.create(
						GoogleCredentials.fromStream(
								gcpPubSubProperties.getCredentials().getLocation().getInputStream())
								.createScoped(gcpPubSubProperties.getCredentials().getScopes()))
				: credentialsProvider;
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherExecutorProvider")
	public ExecutorProvider publisherExecutorProvider() {
		return FixedExecutorProvider.create(Executors.newScheduledThreadPool(
				this.gcpPubSubProperties.getPublisherExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberExecutorProvider")
	public ExecutorProvider subscriberExecutorProvider() {
		return FixedExecutorProvider.create(Executors.newScheduledThreadPool(
				this.gcpPubSubProperties.getSubscriberExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberChannelProvider")
	public TransportChannelProvider subscriberChannelProvider() {
		return SubscriptionAdminSettings.defaultGrpcTransportProviderBuilder()
				.setHeaderProvider(() -> Collections.singletonMap(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion()))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherChannelProvider")
	public TransportChannelProvider publisherChannelProvider() {
		return TopicAdminSettings.defaultGrpcTransportProviderBuilder()
				.setHeaderProvider(() -> Collections.singletonMap(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion()))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubTemplate pubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory) {
		return new PubSubTemplate(publisherFactory, subscriberFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("publisherChannelProvider") TransportChannelProvider channelProvider) {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setChannelProvider(channelProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);

		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(
			@Qualifier("subscriberExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("subscriberChannelProvider") TransportChannelProvider channelProvider) {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setChannelProvider(channelProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);

		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubAdmin pubSubAdmin(TopicAdminClient topicAdminClient,
			SubscriptionAdminClient subscriptionAdminClient) {
		return new PubSubAdmin(this.finalProjectIdProvider, topicAdminClient,
				subscriptionAdminClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public TopicAdminClient topicAdminClient() {
		try {
			return TopicAdminClient.create(
					TopicAdminSettings.newBuilder()
							.setCredentialsProvider(this.finalCredentialsProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating TopicAdminClient.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriptionAdminClient subscriptionAdminClient() {
		try {
			return SubscriptionAdminClient.create(
					SubscriptionAdminSettings.newBuilder()
							.setCredentialsProvider(this.finalCredentialsProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating SubscriptionAdminClient.",
					ioe);
		}
	}
}
