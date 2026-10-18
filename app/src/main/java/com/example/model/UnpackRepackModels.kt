package com.example.model

import java.io.File

enum class RomTargetType(val label: String, val badgeColor: Long) {
    PAYLOAD_BIN("Android OTA Payload (CrAU)", 0xFF005FB0),
    SUPER_IMG("Dynamic Partitions (super.img)", 0xFF6E4EA1),
    SPARSE_IMG("Android Sparse Image", 0xFF006C4C),
    RAW_EXT4("Partition Raw EXT4", 0xFFE67E22),
    EROFS("Partition EROFS (Read-Only)", 0xFF00796B),
    BOOT_IMAGE("Android Boot / Ramdisk", 0xFFA43C2D),
    RECOVERY_ZIP("Archive Flashable (ZIP)", 0xFF455A64),
    DAT_BR("Sparse Dat Brotli (.new.dat.br)", 0xFF6D4C41),
    GENERIC_IMG("Image Android Générique", 0xFF546E7A),
    UNKNOWN("Format Non Reconnu", 0xFF757575)
}

data class PartitionInfo(
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val fsType: String,
    val offset: Long = 0L,
    val sha256: String = "",
    val isSparse: Boolean = false,
    val isSelected: Boolean = true
)

data class TargetMetadata(
    val targetName: String,
    val originalFileName: String,
    val originalPath: String,
    val fileSizeBytes: Long,
    val formattedFileSize: String,
    val sha256: String,
    val targetType: RomTargetType,
    val detectedFs: String,
    val isSparse: Boolean = false,
    val partitions: List<PartitionInfo> = emptyList(),
    val symlinks: List<String> = emptyList(),
    val fsConfigEntries: List<String> = emptyList(),
    val avbFlags: String? = null,
    val blockSize: Int = 4096,
    val unpackTimestamp: Long = System.currentTimeMillis(),
    val unpackDirectory: String = ""
)

data class UnpackUiState(
    val selectedFile: File? = null,
    val targetMetadata: TargetMetadata? = null,
    val isAnalyzing: Boolean = false,
    val isExtracting: Boolean = false,
    val currentStep: String = "",
    val progress: Float = 0f,
    val recentLogs: List<String> = emptyList(),
    val unpackedTargetDirectory: File? = null,
    val lastError: String? = null,
    val successMessage: String? = null
)

data class RepackUiState(
    val availableUnpackedTargets: List<String> = emptyList(),
    val selectedTargetName: String? = null,
    val targetMetadata: TargetMetadata? = null,
    val isRepacking: Boolean = false,
    val repackProgress: Float = 0f,
    val currentStep: String = "",
    val repackLogs: List<String> = emptyList(),
    val repackFormat: String = "AUTO", // AUTO, EXT4, EROFS, SPARSE, SUPER
    val enableSparse: Boolean = true,
    val patchAvb: Boolean = false,
    val lastPackedFile: File? = null,
    val fidelityCheckResult: String? = null,
    val lastError: String? = null,
    val successMessage: String? = null
)
