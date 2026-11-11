package com.example.ptloader

import java.io.File

data class ModInfo(
    val id: String,
    val name: String,
    val author: String,
    val patchFile: String?,
    val dir: File,
)
