package com.bazaaring.k8s

import io.kubernetes.client.informer.SharedIndexInformer
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.util.wait.Wait
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.time.Duration
import java.util.function.Supplier

class K8sInformerClient(
    private val factory: SharedInformerFactory,
    private val deploymentsInformer: SharedIndexInformer<V1Deployment>,
    private val deploymentsLister: Lister<V1Deployment>,
    private val podsInformer: SharedIndexInformer<V1Pod>,
    private val podsLister: Lister<V1Pod>,
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterPropertiesSet() {
        factory.startAllRegisteredInformers()
        val condition = Supplier<Boolean> {
            deploymentsInformer.hasSynced() && podsInformer.hasSynced()
        }
        if (Wait.poll(Duration.ofSeconds(1L), Duration.ofSeconds(60L), condition)) {
            logger.info("k8s cache fully loaded")
        } else {
            throw Exception("k8s cache load timed out")
        }
    }

    fun getDeploymentReplicas(name: String): Int? {
        return deploymentsLister.list()
            .firstOrNull { it.metadata?.name == name }
            ?.spec?.replicas
    }

    fun getPods(name: String): List<K8sPod> {
        return podsLister.list().asSequence()
            .filter { it.filterByName(name) }
            .mapNotNull { it.toDto() }
            .toList()
    }

    private fun V1Pod.filterByName(name: String): Boolean {
        return status?.containerStatuses?.any { it.name == name } ?: false
    }

    private fun V1Pod.toDto(): K8sPod? {
        return K8sPod(
            uid = metadata?.uid ?: return null,
            ip = status?.podIP ?: return null,
        )
    }

}
