package com.example.model

import java.io.File

data class CertDetails(
    val certIndex: Int = 1,
    val certType: String = "X.509",
    val certVersion: Int = 3,
    val serialNumber: String = "",
    val subjectDn: String = "",
    val issuerDn: String = "",
    val isSelfSigned: Boolean = true,
    val validFrom: String = "",
    val validUntil: String = "",
    val sigAlgName: String = "",
    val hashCodeHex: String = "",
    val hashCodeInt: Int = 0,
    val crc32Hex: String = "",
    val crc32Long: Long = 0L,
    val md5Hex: String = "",
    val sha1Hex: String = "",
    val sha224Hex: String = "",
    val sha256Hex: String = "",
    val sha384Hex: String = "",
    val sha512Hex: String = "",
    val certBase64: String = "",
    val signatureBytesHex: String = "",
    val publicKeyString: String = "",
    val signatureBase64: String = ""
)

data class ApkSignatureInfo(
    val relativePath: String,
    val absolutePath: String,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val interfaceVersion: String = "Interface v16 (36)",
    val signatureSchemes: List<String> = emptyList(), // e.g. ["V1", "V2", "V3"]
    val schemesDisplay: String = "Non signé",
    val isSigned: Boolean = false,
    val certificates: List<CertDetails> = emptyList(),
    val rawTextReport: String = "",
    val sharedUserId: String? = null,
    val packageName: String? = null
)

data class ElfBinaryInfo(
    val relativePath: String,
    val absolutePath: String,
    val fileName: String,
    val fileSize: Long,
    val formattedSize: String,
    val isElf: Boolean = true,
    val bitness: Int = 64, // 32 or 64
    val architecture: String = "AArch64", // ARM, AArch64, x86, x86_64
    val soname: String? = null,
    val neededLibraries: List<String> = emptyList(),
    val sha256: String = "",
    val md5: String = "",
    val isHal: Boolean = false,
    val halType: String? = null // HIDL, AIDL, Passthrough, VendorBlob
)

data class TargetTreeEntry(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val formattedSize: String = "",
    val permissions: String = "rwxr-xr-x",
    val sha256: String? = null,
    val childrenCount: Int = 0
)

data class AnalystReportSummary(
    val targetName: String,
    val targetPath: String,
    val exportDir: String,
    val totalFiles: Int = 0,
    val totalDirectories: Int = 0,
    val totalBytes: Long = 0L,
    val formattedTotalSize: String = "0 B",
    val totalApks: Int = 0,
    val signedApksCount: Int = 0,
    val totalElfBinaries: Int = 0,
    val totalHalsFound: Int = 0,
    val analysisDurationMs: Long = 0L,
    val timestamp: String = "",
    val reportFilesGenerated: List<String> = emptyList()
)

data class AnalystUiState(
    val isAnalyzing: Boolean = false,
    val availableTargets: List<String> = emptyList(),
    val selectedTarget: String = "",
    val progress: Float = 0f,
    val currentStepText: String = "Sélectionnez un projet et lancez l'analyse",
    val logs: List<TerminalEntry> = emptyList(),
    val lastSummary: AnalystReportSummary? = null,
    val previewReportText: String = ""
)
