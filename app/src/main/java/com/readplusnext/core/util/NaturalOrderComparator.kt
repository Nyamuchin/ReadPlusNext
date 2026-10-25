package com.readplusnext.core.util

/**
 * Compares strings so that embedded digit runs are ordered by value:
 * `page_2` sorts before `page_10`, `chapter 3` before `chapter 11`.
 */
object NaturalOrderComparator : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val startA = i
                val startB = j
                while (i < a.length && a[i].isDigit()) i++
                while (j < b.length && b[j].isDigit()) j++
                val digitsA = a.substring(startA, i).trimStart('0')
                val digitsB = b.substring(startB, j).trimStart('0')
                if (digitsA.length != digitsB.length) {
                    return digitsA.length - digitsB.length
                }
                val digitComparison = digitsA.compareTo(digitsB)
                if (digitComparison != 0) {
                    return digitComparison
                }
            } else {
                val comparison = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (comparison != 0) {
                    return comparison
                }
                i++
                j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
