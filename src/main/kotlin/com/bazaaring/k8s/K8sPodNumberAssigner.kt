package com.bazaaring.k8s

import com.bazaaring.k8s.utils.hashing.ConsistentHashing
import com.bazaaring.k8s.utils.net.findLocalAddresses
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import java.time.Duration

class K8sInstanceNumberAssigner(
    refreshInterval: Duration,
    private var selfAddress: String = "",
    private val client: K8sInformerClient,
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val cache = Caffeine.newBuilder()
        .refreshAfterWrite(refreshInterval)
        .build<Pair<String, Int>, Int> { (name, numInstances) ->
            computeInstanceNumber(name, numInstances)
        }

    override fun afterPropertiesSet() {
        if (selfAddress.isEmpty())
            selfAddress = findSelfAddress()
        logger.info("self address = $selfAddress")
    }

    private fun findSelfAddress(): String {
        val localAddresses = findLocalAddresses()
        if (localAddresses.isEmpty())
            error("no local address have been found")
        if (localAddresses.size > 1)
            logger.warn("more than one local address have been found = $localAddresses")
        return localAddresses.last()
    }

    private fun computeInstanceNumber(name: String, numInstances: Int): Int {
        val replicas = requireNotNull(client.getDeploymentReplicas(name)) { "deployment ($name) not found" }
        val pods = client.getPods(name)
        val self = pods.firstOrNull { it.ip == selfAddress }
        requireNotNull(self) { "no pod matching ip address ($selfAddress) found" }
        return ConsistentHashing.assignKey(
            numKeys = numInstances,
            numNodes = replicas,
            availableNodes = pods.map { it.uid },
            targetNode = self.uid,
        )
    }

    fun getInstanceNumber(deployment: String, numInstances: Int): Int {
        return cache.get(deployment to numInstances)
    }

}
