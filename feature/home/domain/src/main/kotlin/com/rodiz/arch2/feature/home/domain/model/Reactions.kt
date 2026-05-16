package com.rodiz.arch2.feature.home.domain.model

data class Reactions(
    val likeCount: Int,
    val loveCount: Int,
    val hahaCount: Int,
    val wowCount: Int,
    val sadCount: Int,
    val angryCount: Int,
) {
    val total: Int
        get() = likeCount + loveCount + hahaCount + wowCount + sadCount + angryCount

    companion object {
        val Empty = Reactions(0, 0, 0, 0, 0, 0)
    }
}
