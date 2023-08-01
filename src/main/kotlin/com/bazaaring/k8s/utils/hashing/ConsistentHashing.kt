package com.bazaaring.k8s.utils.hashing

import org.ishugaliy.allgood.consistent.hash.HashRing
import org.ishugaliy.allgood.consistent.hash.hasher.DefaultHasher
import org.ishugaliy.allgood.consistent.hash.hasher.Hasher
import org.ishugaliy.allgood.consistent.hash.node.SimpleNode
import kotlin.jvm.optionals.getOrNull

object ConsistentHashing {

    fun assignKey(numKeys: Int, numNodes: Int, availableNodes: List<String>, targetNode: String): Int {

        require(numKeys >= numNodes) {
            "number of nodes ($numNodes) cannot be greater that number of keys ($numKeys)"
        }

        val hashFn = Hasher { key, seed ->
            if (key.substringAfter(':') in availableNodes) {
                (DefaultHasher.MURMUR_3.hash(key, seed) and Long.MAX_VALUE) % numKeys
            } else {
                key.toLong()
            }
        }

        val ring = HashRing.newBuilder<SimpleNode>()
            .hasher(hashFn)
            .partitionRate(numKeys / numNodes)
            .build()

        for (node in availableNodes)
            ring.add(SimpleNode.of(node))

        repeat(numKeys) {
            val located = ring.locate(it.toString()).getOrNull()?.key
            if (located == targetNode)
                return it
        }

        error("no key assigned to the target node ($targetNode)")
    }

}
