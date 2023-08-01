package com.bazaaring.k8s

import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.util.ClientBuilder
import io.kubernetes.client.util.generic.GenericKubernetesApi
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnProperty(value = ["spring.cloud.k8s.enabled"], matchIfMissing = true)
@EnableConfigurationProperties(K8sProperties::class)
class K8sAutoConfiguration(private val props: K8sProperties) {

    @Bean
    fun k8sApiClient(): ApiClient {
        return runCatching { ClientBuilder.cluster().build() }
            .getOrElse { ClientBuilder.defaultClient() }
    }

    @Bean
    fun sharedInformerFactory(apiClient: ApiClient): SharedInformerFactory {
        return SharedInformerFactory(apiClient)
    }

    @Bean
    fun deploymentsSharedIndexInformer(
        factory: SharedInformerFactory,
        apiClient: ApiClient
    ): SharedIndexInformer<V1Deployment> {
        val api = GenericKubernetesApi(
            V1Deployment::class.java, V1DeploymentList::class.java, "apps", "v1", "deployments", apiClient
        )
        return factory.sharedIndexInformerFor(
            api,
            V1Deployment::class.java,
            props.informerResyncPeriod.toMillis(),
            props.namespace,
        )
    }

    @Bean
    fun podsSharedIndexInformer(factory: SharedInformerFactory, apiClient: ApiClient): SharedIndexInformer<V1Pod> {
        val api = GenericKubernetesApi(
            V1Pod::class.java, V1PodList::class.java, "", "v1", "pods", apiClient
        )
        return factory.sharedIndexInformerFor(
            api,
            V1Pod::class.java,
            props.informerResyncPeriod.toMillis(),
            props.namespace,
        )
    }

    @Bean
    fun deploymentsLister(informer: SharedIndexInformer<V1Deployment>): Lister<V1Deployment> {
        return Lister(informer.indexer, props.namespace)
    }

    @Bean
    fun podsLister(informer: SharedIndexInformer<V1Pod>): Lister<V1Pod> {
        return Lister(informer.indexer, props.namespace)
    }

    @Bean
    fun informerClient(
        factory: SharedInformerFactory,
        deploymentsInformer: SharedIndexInformer<V1Deployment>,
        deploymentsLister: Lister<V1Deployment>,
        podsInformer: SharedIndexInformer<V1Pod>,
        podsLister: Lister<V1Pod>,
    ): K8sInformerClient {
        return K8sInformerClient(factory, deploymentsInformer, deploymentsLister, podsInformer, podsLister)
    }

    @Bean
    fun instanceNumberAssigner(client: K8sInformerClient): K8sInstanceNumberAssigner {
        return K8sInstanceNumberAssigner(refreshInterval = props.instanceNumberAssignerRefreshInterval, client = client)
    }

}
