package com.bazaaring.k8s

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("spring.cloud.k8s")
class K8sProperties {
    var namespace = ""
    var informerResyncPeriod: Duration = Duration.ofSeconds(5L)
    var instanceNumberAssignerRefreshInterval: Duration = Duration.ofSeconds(2L)
}
