package com.example.classora.data

data class SemesterResult(
    val semester: String,
    val gpa: String,
    val status: String // "Completed", "Ongoing"
)

data class AcademicData(
    val currentCgpa: String = "0.0",
    val targetCgpa: String = "8.5",
    val semesterResults: List<SemesterResult> = emptyList()
)
