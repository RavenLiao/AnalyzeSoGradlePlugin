package io.github.ravenliao.plugin

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

internal object DependencyGraphUtils {

    data class IntroducedByResult(
        val introducedBy: Map<String, List<String>>,
        val introducedByPaths: Map<String, Map<String, List<String>>>
    )

    fun computeIntroducedBy(root: ResolvedComponentResult): IntroducedByResult {
        val topLevelComponents = root.dependencies
            .asSequence()
            .filterIsInstance<ResolvedDependencyResult>()
            .filter { !it.isConstraint }
            .map { it.selected }
            .distinctBy { it.id.displayName }
            .toList()

        val introducedBy = mutableMapOf<String, MutableSet<String>>()
        val introducedByPaths = mutableMapOf<String, MutableMap<String, List<String>>>()

        for (top in topLevelComponents) {
            val topName = top.id.displayName

            val parent = mutableMapOf<String, String?>()
            val seen = mutableSetOf<String>()
            parent[topName] = null
            seen.add(topName)

            val queue = ArrayDeque<ResolvedComponentResult>()
            queue.addLast(top)

            while (queue.isNotEmpty()) {
                val component = queue.removeFirst()
                val componentName = component.id.displayName

                introducedBy.getOrPut(componentName) { mutableSetOf() }.add(topName)

                component.dependencies
                    .asSequence()
                    .filterIsInstance<ResolvedDependencyResult>()
                    .filter { !it.isConstraint }
                    .forEach { dep ->
                        val child = dep.selected
                        val childName = child.id.displayName
                        if (seen.add(childName)) {
                            parent[childName] = componentName
                            queue.addLast(child)
                        }
                    }
            }

            for (nodeName in parent.keys) {
                val path = buildPathToTop(nodeName, topName, parent)
                introducedByPaths.getOrPut(nodeName) { mutableMapOf() }[topName] = path
            }
        }

        val introducedByFinal = introducedBy.mapValues { (_, v) -> v.toList().sorted() }
        return IntroducedByResult(introducedByFinal, introducedByPaths)
    }

    private fun buildPathToTop(
        nodeName: String,
        topName: String,
        parent: Map<String, String?>
    ): List<String> {
        val path = ArrayList<String>()
        var current: String? = nodeName
        while (current != null) {
            path.add(current)
            if (current == topName) break
            current = parent[current]
        }
        path.reverse()
        return path
    }
}
