package com.zenzone.app.model

data class ZenBadge(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: String,
    val requiredChain: Int,
    val isEarned: Boolean
)
