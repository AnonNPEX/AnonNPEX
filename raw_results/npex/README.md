## NPEX Data
* input of NPEX
  * Stack trace: [bug_id]/input/traces.json
  * NPE-triggering expression: [bug_id]/input/npe.json
* output of NPEX
  * patch candidates: [bug_id]/patches/[patch_id]/patch.java
  * validation results: [bug_id]/README.md

## Summary of NPEX's Results
| Bud ID                               | Source  | Label     | Detail Results                                              |
|--------------------------------------|---------|-----------|-------------------------------------------------------------|
| Bears-56-buggy                       | Bears   | NO_PATCH  | [Results](./Bears-56-buggy/README.md)                       |
| Bears-121-buggy                      | Bears   | CORRECT   | [Results](./Bears-121-buggy/README.md)                      |
| Bears-17-buggy                       | Bears   | CORRECT   | [Results](./Bears-17-buggy/README.md)                       |
| Bears-184-buggy                      | Bears   | CORRECT   | [Results](./Bears-184-buggy/README.md)                      |
| Bears-196-buggy                      | Bears   | CORRECT   | [Results](./Bears-196-buggy/README.md)                      |
| Bears-32-buggy                       | Bears   | CORRECT   | [Results](./Bears-32-buggy/README.md)                       |
| Bears-46-buggy                       | Bears   | CORRECT   | [Results](./Bears-46-buggy/README.md)                       |
| Bears-180-buggy                      | Bears   | NO_PATCH  | [Results](./Bears-180-buggy/README.md)                      |
| Bears-188-buggy                      | Bears   | INCORRECT | [Results](./Bears-188-buggy/README.md)                      |
| Bears-189-buggy                      | Bears   | NO_PATCH  | [Results](./Bears-189-buggy/README.md)                      |
| Bears-200-buggy                      | Bears   | NO_PATCH  | [Results](./Bears-200-buggy/README.md)                      |
| Bears-222-buggy                      | Bears   | INCORRECT | [Results](./Bears-222-buggy/README.md)                      |
| Bears-249-buggy                      | Bears   | NO_PATCH  | [Results](./Bears-249-buggy/README.md)                      |
| Bears-88-buggy                       | Bears   | NO_PATCH  | [Results](./Bears-88-buggy/README.md)                       |
| Activity-3d624a5-buggy               | Genesis | CORRECT   | [Results](./Activity-3d624a5-buggy/README.md)               |
| DataflowJavaSDK-c06125d-buggy        | Genesis | CORRECT   | [Results](./DataflowJavaSDK-c06125d-buggy/README.md)        |
| caelum-stella-2d2dd9c-buggy          | Genesis | CORRECT   | [Results](./caelum-stella-2d2dd9c-buggy/README.md)          |
| caelum-stella-2ec5459-buggy          | Genesis | CORRECT   | [Results](./caelum-stella-2ec5459-buggy/README.md)          |
| caelum-stella-e73113f-buggy          | Genesis | CORRECT   | [Results](./caelum-stella-e73113f-buggy/README.md)          |
| checkstyle-536bc20-buggy             | Genesis | CORRECT   | [Results](./checkstyle-536bc20-buggy/README.md)             |
| checkstyle-8381754-buggy             | Genesis | CORRECT   | [Results](./checkstyle-8381754-buggy/README.md)             |
| error-prone-370938-buggy             | Genesis | CORRECT   | [Results](./error-prone-370938-buggy/README.md)             |
| javaslang-faf9ac2-buggy              | Genesis | CORRECT   | [Results](./javaslang-faf9ac2-buggy/README.md)              |
| spring-hateoas-48749e7-buggy         | Genesis | CORRECT   | [Results](./spring-hateoas-48749e7-buggy/README.md)         |
| HikariCP-ce4ff92-buggy               | Genesis | NO_PATCH  | [Results](./HikariCP-ce4ff92-buggy/README.md)               |
| checkstyle-aa829d4-buggy             | Genesis | INCORRECT | [Results](./checkstyle-aa829d4-buggy/README.md)             |
| checkstyle-aaf606e-buggy             | Genesis | INCORRECT | [Results](./checkstyle-aaf606e-buggy/README.md)             |
| javapoet-70b38e5-buggy               | Genesis | INCORRECT | [Results](./javapoet-70b38e5-buggy/README.md)               |
| jongo-f46f658-buggy                  | Genesis | NO_PATCH  | [Results](./jongo-f46f658-buggy/README.md)                  |
| truth-99b314e-buggy                  | Genesis | INCORRECT | [Results](./truth-99b314e-buggy/README.md)                  |
| cxf_aebfb0d                          | Ours    | NO_PATCH  | [Results](./cxf_aebfb0d/README.md)                          |
| jsoup_8808e3f                        | Ours    | INCORRECT | [Results](./jsoup_8808e3f/README.md)                        |
| avro_a7a43da                         | Ours    | NO_PATCH  | [Results](./avro_a7a43da/README.md)                         |
| johnzon_8b5c87d                      | Ours    | INCORRECT | [Results](./johnzon_8b5c87d/README.md)                      |
| karaf_5965290                        | Ours    | INCORRECT | [Results](./karaf_5965290/README.md)                        |
| nutz_c8a9e83                         | Ours    | NO_PATCH  | [Results](./nutz_c8a9e83/README.md)                         |
| shardingsphere_52b0150               | Ours    | NO_PATCH  | [Results](./shardingsphere_52b0150/README.md)               |
| JSqlParser_2897935                   | Ours    | CORRECT   | [Results](./JSqlParser_2897935/README.md)                   |
| OpenPDF_07ecaaf                      | Ours    | CORRECT   | [Results](./OpenPDF_07ecaaf/README.md)                      |
| activemq-artemis_6fbafc4             | Ours    | CORRECT   | [Results](./activemq-artemis_6fbafc4/README.md)             |
| aries-jpa_7712046                    | Ours    | CORRECT   | [Results](./aries-jpa_7712046/README.md)                    |
| async-http-client_86948f6            | Ours    | CORRECT   | [Results](./async-http-client_86948f6/README.md)            |
| commons-configuration_746821e        | Ours    | CORRECT   | [Results](./commons-configuration_746821e/README.md)        |
| commons-dbcp_b137fda                 | Ours    | CORRECT   | [Results](./commons-dbcp_b137fda/README.md)                 |
| commons-io_1ac7bef                   | Ours    | CORRECT   | [Results](./commons-io_1ac7bef/README.md)                   |
| commons-pool_41f4e41                 | Ours    | CORRECT   | [Results](./commons-pool_41f4e41/README.md)                 |
| cxf_ae805e6                          | Ours    | CORRECT   | [Results](./cxf_ae805e6/README.md)                          |
| directory-ldap-api_3c6a765           | Ours    | CORRECT   | [Results](./directory-ldap-api_3c6a765/README.md)           |
| dubbo-hessian-lite_5526dd8           | Ours    | CORRECT   | [Results](./dubbo-hessian-lite_5526dd8/README.md)           |
| feign_cf31cd1                        | Ours    | CORRECT   | [Results](./feign_cf31cd1/README.md)                        |
| httpcomponents-core_a63b121          | Ours    | CORRECT   | [Results](./httpcomponents-core_a63b121/README.md)          |
| iotdb_97ca7e0                        | Ours    | CORRECT   | [Results](./iotdb_97ca7e0/README.md)                        |
| jspwiki_bdab6f2                      | Ours    | CORRECT   | [Results](./jspwiki_bdab6f2/README.md)                      |
| logging-log4j2_d1c02ee               | Ours    | CORRECT   | [Results](./logging-log4j2_d1c02ee/README.md)               |
| ninja_3e73cb8                        | Ours    | CORRECT   | [Results](./ninja_3e73cb8/README.md)                        |
| nutz_bbd28db                         | Ours    | CORRECT   | [Results](./nutz_bbd28db/README.md)                         |
| opennlp_cb6ee2c                      | Ours    | CORRECT   | [Results](./opennlp_cb6ee2c/README.md)                      |
| pdfbox_bdab232                       | Ours    | CORRECT   | [Results](./pdfbox_bdab232/README.md)                       |
| rocketmq_03c1f11                     | Ours    | CORRECT   | [Results](./rocketmq_03c1f11/README.md)                     |
| shardingsphere_e46f68d               | Ours    | CORRECT   | [Results](./shardingsphere_e46f68d/README.md)               |
| shiro_3ca513f                        | Ours    | CORRECT   | [Results](./shiro_3ca513f/README.md)                        |
| sling-org-apache-sling-pipes_674819d | Ours    | CORRECT   | [Results](./sling-org-apache-sling-pipes_674819d/README.md) |
| tablesaw_65596d8                     | Ours    | CORRECT   | [Results](./tablesaw_65596d8/README.md)                     |
| Activiti_0d83e98                     | Ours    | INCORRECT | [Results](./Activiti_0d83e98/README.md)                     |
| Java-WebSocket_51b63a2               | Ours    | INCORRECT | [Results](./Java-WebSocket_51b63a2/README.md)               |
| Jest_93f3c48                         | Ours    | INCORRECT | [Results](./Jest_93f3c48/README.md)                         |
| chart-2                              | Ours    | NO_PATCH  | [Results](./chart-2/README.md)                              |
| client_java_422e3b3                  | Ours    | INCORRECT | [Results](./client_java_422e3b3/README.md)                  |
| commons-bcel_1faf7d9                 | Ours    | NO_PATCH  | [Results](./commons-bcel_1faf7d9/README.md)                 |
| commons-collections_a270ff6          | Ours    | NO_PATCH  | [Results](./commons-collections_a270ff6/README.md)          |
| easy-rules_72d7bff                   | Ours    | NO_PATCH  | [Results](./easy-rules_72d7bff/README.md)                   |
| fastjson_7c05c6f                     | Ours    | NO_PATCH  | [Results](./fastjson_7c05c6f/README.md)                     |
| httpcomponents-client_65c6c25        | Ours    | NO_PATCH  | [Results](./httpcomponents-client_65c6c25/README.md)        |
| httpcomponents-client_bf1822c        | Ours    | INCORRECT | [Results](./httpcomponents-client_bf1822c/README.md)        |
| incubator-hivemall_e86815a           | Ours    | NO_PATCH  | [Results](./incubator-hivemall_e86815a/README.md)           |
| jsoup_1d663ee                        | Ours    | INCORRECT | [Results](./jsoup_1d663ee/README.md)                        |
| logging-log4j2_147f78c               | Ours    | INCORRECT | [Results](./logging-log4j2_147f78c/README.md)               |
| logging-log4j2_2d266d9               | Ours    | INCORRECT | [Results](./logging-log4j2_2d266d9/README.md)               |
| logging-log4j2_cd881ce               | Ours    | INCORRECT | [Results](./logging-log4j2_cd881ce/README.md)               |
| math-70                              | Ours    | NO_PATCH  | [Results](./math-70/README.md)                              |
| math-79                              | Ours    | NO_PATCH  | [Results](./math-79/README.md)                              |
| maven-doxia_16836d8                  | Ours    | INCORRECT | [Results](./maven-doxia_16836d8/README.md)                  |
| maven-release_6b1f658                | Ours    | INCORRECT | [Results](./maven-release_6b1f658/README.md)                |
| maven-surefire_08163fa               | Ours    | INCORRECT | [Results](./maven-surefire_08163fa/README.md)               |
| opengrok_6a95adb                     | Ours    | INCORRECT | [Results](./opengrok_6a95adb/README.md)                     |
| pdfbox-2266                          | Ours    | INCORRECT | [Results](./pdfbox-2266/README.md)                          |
| pdfbox-2812                          | Ours    | NO_PATCH  | [Results](./pdfbox-2812/README.md)                          |
| pdfbox-2951                          | Ours    | INCORRECT | [Results](./pdfbox-2951/README.md)                          |
| picocli_758f4c0                      | Ours    | INCORRECT | [Results](./picocli_758f4c0/README.md)                      |
| qpid-proton-j_6422e24                | Ours    | NO_PATCH  | [Results](./qpid-proton-j_6422e24/README.md)                |
| qpid-proton-j_87ec94b                | Ours    | INCORRECT | [Results](./qpid-proton-j_87ec94b/README.md)                |
| shardingsphere_48237c8               | Ours    | NO_PATCH  | [Results](./shardingsphere_48237c8/README.md)               |
| xmlgraphics-fop_62ff114              | Ours    | INCORRECT | [Results](./xmlgraphics-fop_62ff114/README.md)              |
| xmlgraphics-fop_991b446              | Ours    | INCORRECT | [Results](./xmlgraphics-fop_991b446/README.md)              |
| zookeeper_e41cac8                    | Ours    | INCORRECT | [Results](./zookeeper_e41cac8/README.md)                    |
| chart-14                             | VFix    | CORRECT   | [Results](./chart-14/README.md)                             |
| chart-15                             | VFix    | CORRECT   | [Results](./chart-15/README.md)                             |
| chart-26                             | VFix    | CORRECT   | [Results](./chart-26/README.md)                             |
| chart-4                              | VFix    | CORRECT   | [Results](./chart-4/README.md)                              |
| collections-360                      | VFix    | CORRECT   | [Results](./collections-360/README.md)                      |
| felix-4960                           | VFix    | CORRECT   | [Results](./felix-4960/README.md)                           |
| felix-5464                           | VFix    | CORRECT   | [Results](./felix-5464/README.md)                           |
| lang-20                              | VFix    | CORRECT   | [Results](./lang-20/README.md)                              |
| lang-33                              | VFix    | CORRECT   | [Results](./lang-33/README.md)                              |
| lang-39                              | VFix    | CORRECT   | [Results](./lang-39/README.md)                              |
| lang-47                              | VFix    | CORRECT   | [Results](./lang-47/README.md)                              |
| math-4                               | VFix    | CORRECT   | [Results](./math-4/README.md)                               |
| pdfbox-2477                          | VFix    | CORRECT   | [Results](./pdfbox-2477/README.md)                          |
| pdfbox-2948                          | VFix    | CORRECT   | [Results](./pdfbox-2948/README.md)                          |
| pdfbox-2965                          | VFix    | CORRECT   | [Results](./pdfbox-2965/README.md)                          |
| pdfbox-2995                          | VFix    | CORRECT   | [Results](./pdfbox-2995/README.md)                          |
| pdfbox-3572                          | VFix    | CORRECT   | [Results](./pdfbox-3572/README.md)                          |
| sling-4982                           | VFix    | CORRECT   | [Results](./sling-4982/README.md)                           |
| sling-6487                           | VFix    | CORRECT   | [Results](./sling-6487/README.md)                           |
| chart-16                             | VFix    | INCORRECT | [Results](./chart-16/README.md)                             |
| chart-25                             | VFix    | NO_PATCH  | [Results](./chart-25/README.md)                             |
| collections-39                       | VFix    | NO_PATCH  | [Results](./collections-39/README.md)                       |
| lang-57                              | VFix    | INCORRECT | [Results](./lang-57/README.md)                              |
| pdfbox-3479                          | VFix    | NO_PATCH  | [Results](./pdfbox-3479/README.md)                          |
