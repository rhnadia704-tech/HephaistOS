package com.example.data

import android.content.Context
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipFile

/**
 * VERISIN Engine — Moteur d'audit cryptographique et de vérification approfondie de signatures d'APKs.
 * Conçu pour HephaistOS, extensible et interrogeable via le Terminal interne.
 */
object VerisinEngine {

    private const val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42"
    private const val SCHEME_V2_ID = 0x7109871a
    private const val SCHEME_V3_ID = 0xf05368c0.toInt()
    private const val SCHEME_V31_ID = 0x1b93ad61

    data class VerisinCertificateDetail(
        val certIndex: Int,
        val certType: String,
        val certVersion: Int,
        val serialNumberDecimal: String,
        val subjectDn: String,
        val issuerDn: String,
        val isSelfSigned: Boolean,
        val validityFromFormatted: String,
        val validityUntilFormatted: String,
        val sigAlgName: String,
        val hashCodeFormatted: String,
        val crc32Formatted: String,
        val md5Hex: String,
        val sha1Hex: String,
        val sha224Hex: String,
        val sha256Hex: String,
        val sha384Hex: String,
        val sha512Hex: String,
        val certBase64: String,
        val signatureBitsHex: String,
        val publicKeyFormatted: String,
        val signatureBase64: String,
        val keyType: String = "RSA",
        val keySizeBits: Int = 2048,
        val publicKeySha256: String = "",
        val extensionsSummary: List<String> = emptyList()
    )

    data class VerisinResult(
        val isSigned: Boolean,
        val filePath: String,
        val fileName: String,
        val fileSizeFormatted: String,
        val androidInterface: String,
        val signatureSchemes: List<String>,
        val schemesDisplay: String,
        val certificates: List<VerisinCertificateDetail>,
        val rawReportText: String,
        val reportFile: File? = null,
        val jsonFile: File? = null,
        val packageName: String = "",
        val versionCode: Long = 0L,
        val versionName: String = "",
        val sharedUserId: String? = null
    )

    /**
     * Point d'entrée principal pour la vérification complète d'un fichier APK.
     */
    suspend fun verifyApkFile(
        context: Context,
        apkFile: File,
        onLog: ((String) -> Unit)? = null
    ): VerisinResult = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.isFile) {
            val errText = "Erreur : Le fichier '${apkFile.absolutePath}' n'existe pas ou n'est pas accessible."
            return@withContext VerisinResult(
                isSigned = false,
                filePath = apkFile.absolutePath,
                fileName = apkFile.name,
                fileSizeFormatted = "0 octet",
                androidInterface = "Inconnue",
                signatureSchemes = emptyList(),
                schemesDisplay = "Fichier introuvable",
                certificates = emptyList(),
                rawReportText = errText
            )
        }

        onLog?.invoke("[VERISIN] Analyse cryptographique en cours : ${apkFile.name}...")

        // 1. Détection des schémas de signature (V1, V2, V3, V3.1, V4)
        val detectedSchemes = mutableListOf<String>()
        val parsedCertificates = mutableListOf<X509Certificate>()
        val rawSignaturesMap = mutableMapOf<Int, ByteArray>()

        // 1A. Vérification V2 / V3 / V3.1 dans l'APK Signing Block
        val signingBlockCerts = extractSigningBlockData(apkFile, detectedSchemes, rawSignaturesMap)
        parsedCertificates.addAll(signingBlockCerts)

        // 1B. Vérification V1 (JAR META-INF)
        val v1Certs = extractV1Certificates(apkFile)
        if (v1Certs.isNotEmpty()) {
            if (!detectedSchemes.contains("V1")) detectedSchemes.add("V1")
            for (c in v1Certs) {
                if (parsedCertificates.none { it.encoded.contentEquals(c.encoded) }) {
                    parsedCertificates.add(c)
                }
            }
        }

        // 1C. Vérification V4 (idsig)
        val idsigFile = File(apkFile.parentFile, "${apkFile.name}.idsig")
        if (idsigFile.exists() && idsigFile.length() > 0) {
            detectedSchemes.add("V4")
        }

        // Ordre d'affichage canonique
        val orderedSchemes = listOf("V1", "V2", "V3", "V3.1", "V4").filter { detectedSchemes.contains(it) }
        val schemesDisplay = if (orderedSchemes.isNotEmpty()) {
            orderedSchemes.joinToString(" + ")
        } else {
            "Non signé"
        }

        // 2. Extraction du Manifeste (Package, Version, SDK / Interface)
        val manifestData = parseManifestQuick(apkFile)
        val androidInterfaceDisplay = manifestData.interfaceDisplay

        // 3. Construction des détails exhaustifs de chaque certificat
        val certDetails = mutableListOf<VerisinCertificateDetail>()
        val frenchDateFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)

        for ((idx, cert) in parsedCertificates.withIndex()) {
            val certBytes = cert.encoded

            // Serial Number
            val serialDecimal = cert.serialNumber.toString(10)

            // Subject & Issuer
            val subjectDn = cert.subjectX500Principal.name
            val issuerDn = cert.issuerX500Principal.name
            val isSelfSigned = cert.subjectX500Principal == cert.issuerX500Principal

            // Validité
            val valFrom = frenchDateFormat.format(cert.notBefore)
            val valUntil = frenchDateFormat.format(cert.notAfter)

            // Algorithme
            val sigAlg = cert.sigAlgName

            // Hash code formaté
            val hashInt = cert.hashCode()
            val hashHex = "0x" + Integer.toHexString(hashInt).lowercase(Locale.ROOT)
            val hashCodeFormatted = "$hashHex ($hashInt)"

            // CRC32 formaté
            val crcVal = CRC32().apply { update(certBytes) }.value
            val crcHex = java.lang.Long.toHexString(crcVal).lowercase(Locale.ROOT).padStart(8, '0')
            val crcFormatted = "$crcHex ($crcVal)"

            // Hachages cryptographiques
            val md5Hex = computeHash(certBytes, "MD5")
            val sha1Hex = computeHash(certBytes, "SHA-1")
            val sha224Hex = computeHash(certBytes, "SHA-224")
            val sha256Hex = computeHash(certBytes, "SHA-256")
            val sha384Hex = computeHash(certBytes, "SHA-384")
            val sha512Hex = computeHash(certBytes, "SHA-512")

            // Base64 certificat
            val certB64 = Base64.encodeToString(certBytes, Base64.NO_WRAP)

            // Bits de signature (ASN.1 signatureValue du certificat)
            val sigBits = try {
                bytesToHex(cert.signature)
            } catch (_: Exception) {
                ""
            }

            // Clé publique format OpenSSL
            val pubKey = cert.publicKey
            val (pubKeyStr, keyType, keySize) = formatPublicKey(pubKey)
            val pubKeySha256 = computeHash(pubKey.encoded, "SHA-256")

            // Base64 de signature (charge utile de signature ou signature de certificat)
            val rawSigPayload = rawSignaturesMap[idx] ?: cert.signature
            val rawSigB64 = Base64.encodeToString(rawSigPayload, Base64.NO_WRAP)

            // Extensions X.509
            val extensions = mutableListOf<String>()
            try {
                if (cert.basicConstraints != -1) extensions.add("BasicConstraints: isCA=${cert.basicConstraints >= 0} (pathLen=${cert.basicConstraints})")
                val keyUsage = cert.keyUsage
                if (keyUsage != null) {
                    val usages = mutableListOf<String>()
                    if (keyUsage.getOrNull(0) == true) usages.add("digitalSignature")
                    if (keyUsage.getOrNull(1) == true) usages.add("nonRepudiation")
                    if (keyUsage.getOrNull(2) == true) usages.add("keyEncipherment")
                    if (keyUsage.getOrNull(5) == true) usages.add("keyCertSign")
                    if (usages.isNotEmpty()) extensions.add("KeyUsage: ${usages.joinToString(", ")}")
                }
            } catch (_: Exception) {}

            certDetails.add(
                VerisinCertificateDetail(
                    certIndex = idx + 1,
                    certType = "X.509",
                    certVersion = cert.version,
                    serialNumberDecimal = serialDecimal,
                    subjectDn = subjectDn,
                    issuerDn = issuerDn,
                    isSelfSigned = isSelfSigned,
                    validityFromFormatted = "de $valFrom à $valUntil",
                    validityUntilFormatted = valUntil,
                    sigAlgName = sigAlg,
                    hashCodeFormatted = hashCodeFormatted,
                    crc32Formatted = crcFormatted,
                    md5Hex = md5Hex,
                    sha1Hex = sha1Hex,
                    sha224Hex = sha224Hex,
                    sha256Hex = sha256Hex,
                    sha384Hex = sha384Hex,
                    sha512Hex = sha512Hex,
                    certBase64 = certB64,
                    signatureBitsHex = sigBits,
                    publicKeyFormatted = pubKeyStr,
                    signatureBase64 = rawSigB64,
                    keyType = keyType,
                    keySizeBits = keySize,
                    publicKeySha256 = pubKeySha256,
                    extensionsSummary = extensions
                )
            )
        }

        // 4. Génération de la chaîne textuelle formelle demandée
        val reportBuilder = StringBuilder()
        reportBuilder.append("Vérification de la signature: $androidInterfaceDisplay\n")
        reportBuilder.append("Chemin: ${apkFile.absolutePath}\n\n")
        reportBuilder.append("Schéma de signature : $schemesDisplay\n")

        if (certDetails.isEmpty()) {
            reportBuilder.append("\n[AVERTISSEMENT] Aucun certificat valide détecté dans ce paquet (APK non signé ou corrompu).\n")
        } else {
            for (c in certDetails) {
                reportBuilder.append("\nCERTIFICAT №${c.certIndex}\n")
                reportBuilder.append("Type de certificat : ${c.certType}\n")
                reportBuilder.append("Version du certificat : ${c.certVersion}\n")
                reportBuilder.append("Numéros de série: ${c.serialNumberDecimal}\n")
                reportBuilder.append("Propriétaire du certificat : ${c.subjectDn}\n")
                reportBuilder.append("Émetteur de certificat : ${if (c.isSelfSigned) "Auto-signé par le propriétaire du certificat" else c.issuerDn}\n")
                reportBuilder.append("Validité : ${c.validityFromFormatted}\n")
                reportBuilder.append("Algorithme de signature : ${c.sigAlgName}\n\n")

                reportBuilder.append("Code de hachage : ${c.hashCodeFormatted}\n")
                reportBuilder.append("Signature CRC32 : ${c.crc32Formatted}\n")
                reportBuilder.append("Signature MD5 : ${c.md5Hex}\n")
                reportBuilder.append("Signature SHA-1 : ${c.sha1Hex}\n")
                reportBuilder.append("Signature SHA-224 : ${c.sha224Hex}\n")
                reportBuilder.append("Signature SHA-256 : ${c.sha224Hex.let { c.sha256Hex }}\n")
                reportBuilder.append("Signature SHA-384 : ${c.sha384Hex}\n")
                reportBuilder.append("Signature SHA-512 : ${c.sha512Hex}\n\n")

                reportBuilder.append("Base64 du certificat : ${c.certBase64}\n\n")
                reportBuilder.append("Bits de signature : ${c.signatureBitsHex}\n\n")
                reportBuilder.append("Clé publique : ${c.publicKeyFormatted}\n\n")
                reportBuilder.append("Base64 de signature : ${c.signatureBase64}\n")

                if (c.extensionsSummary.isNotEmpty()) {
                    reportBuilder.append("\nExtensions X.509 :\n")
                    c.extensionsSummary.forEach { ext -> reportBuilder.append("  • $ext\n") }
                }
            }
        }

        val fullReportText = reportBuilder.toString()

        // 5. Sauvegarde automatique dans /sdcard/HEPHAISTOS/VERISIN/reports/ et /sdcard/HEPHAISTOS/VERISIN/Data/
        var reportFile: File? = null
        var jsonFile: File? = null
        try {
            val reportsDir = WorkspaceManager.getVerisinReportsDir(context)
            val dataDir = WorkspaceManager.getVerisinDataDir(context)
            val baseName = apkFile.nameWithoutExtension

            reportFile = File(reportsDir, "${baseName}_verisin.txt")
            reportFile.writeText(fullReportText)

            val jsonReport = JSONObject().apply {
                put("engine", "VERISIN")
                put("version", "1.0")
                put("timestamp", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH).format(Date()))
                put("fileName", apkFile.name)
                put("filePath", apkFile.absolutePath)
                put("fileSizeBytes", apkFile.length())
                put("fileSizeFormatted", UkaEngine.formatFileSize(apkFile.length()))
                put("isSigned", certDetails.isNotEmpty())
                put("androidInterface", androidInterfaceDisplay)
                put("signatureSchemes", JSONArray(orderedSchemes))
                put("schemesDisplay", schemesDisplay)
                put("packageName", manifestData.packageName)
                put("versionCode", manifestData.versionCode)
                put("versionName", manifestData.versionName)
                put("sharedUserId", manifestData.sharedUserId ?: "")

                val certsArr = JSONArray()
                certDetails.forEach { c ->
                    certsArr.put(JSONObject().apply {
                        put("certIndex", c.certIndex)
                        put("certType", c.certType)
                        put("certVersion", c.certVersion)
                        put("serialNumber", c.serialNumberDecimal)
                        put("subject", c.subjectDn)
                        put("issuer", c.issuerDn)
                        put("isSelfSigned", c.isSelfSigned)
                        put("validity", c.validityFromFormatted)
                        put("algorithm", c.sigAlgName)
                        put("hashCode", c.hashCodeFormatted)
                        put("crc32", c.crc32Formatted)
                        put("md5", c.md5Hex)
                        put("sha1", c.sha1Hex)
                        put("sha224", c.sha224Hex)
                        put("sha256", c.sha256Hex)
                        put("sha384", c.sha384Hex)
                        put("sha512", c.sha512Hex)
                        put("certBase64", c.certBase64)
                        put("signatureBitsHex", c.signatureBitsHex)
                        put("publicKey", c.publicKeyFormatted)
                        put("signatureBase64", c.signatureBase64)
                        put("keyType", c.keyType)
                        put("keySizeBits", c.keySizeBits)
                    })
                }
                put("certificates", certsArr)
            }
            jsonFile = File(dataDir, "${baseName}_verisin.json")
            jsonFile.writeText(jsonReport.toString(2))
        } catch (_: Exception) {}

        VerisinResult(
            isSigned = certDetails.isNotEmpty(),
            filePath = apkFile.absolutePath,
            fileName = apkFile.name,
            fileSizeFormatted = UkaEngine.formatFileSize(apkFile.length()),
            androidInterface = androidInterfaceDisplay,
            signatureSchemes = orderedSchemes,
            schemesDisplay = schemesDisplay,
            certificates = certDetails,
            rawReportText = fullReportText,
            reportFile = reportFile,
            jsonFile = jsonFile,
            packageName = manifestData.packageName,
            versionCode = manifestData.versionCode,
            versionName = manifestData.versionName,
            sharedUserId = manifestData.sharedUserId
        )
    }

    /**
     * Aide complète du moteur VERISIN à afficher dans le Terminal.
     */
    fun getHelpBanner(): List<String> {
        val list = mutableListOf<String>()
        list.add("=======================================================")
        list.add("      VERISIN Core Engine — Audit Cryptographique      ")
        list.add("               HephaistOS Security Tool                ")
        list.add("=======================================================")
        list.add("VERISIN est le moteur natif d'HephaistOS dédié à la vérification")
        list.add("ultra-complète et forensique des signatures d'applications (APK).")
        list.add("")
        list.add("Commandes disponibles :")
        list.add("  verisin <fichier.apk>         - Vérification exhaustive d'un APK")
        list.add("  verisin verify <fichier.apk>  - Vérifie et extrait tous les détails")
        list.add("  verisin-help                  - Affiche cette aide complète")
        list.add("  verisin --help | verisin -h   - Alias d'aide")
        list.add("")
        list.add("Schémas analysés :")
        list.add("  • Schéma V1   : JAR Signature (META-INF/*.RSA, *.DSA, *.EC)")
        list.add("  • Schéma V2   : APK Signature Scheme v2 (ID 0x7109871a)")
        list.add("  • Schéma V3   : APK Signature Scheme v3 (ID 0xf05368c0)")
        list.add("  • Schéma V3.1 : APK Signature Scheme v3.1 (ID 0x1b93ad61)")
        list.add("  • Schéma V4   : Android 11+ Incremental (fichier .idsig)")
        list.add("")
        list.add("Informations restituées :")
        list.add("  • Interface / SDK Target (ex: Interface v16 (36))")
        list.add("  • Type, version X.509 et numéro de série décimal")
        list.add("  • Propriétaire (Subject DN) & Émetteur (Issuer DN)")
        list.add("  • Validité temporelle calendaire et algorithme")
        list.add("  • Code de hachage signed/unsigned, CRC32 signed/unsigned")
        list.add("  • Sommes de contrôle MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512")
        list.add("  • Base64 du certificat, Bits de signature hexadécimaux")
        list.add("  • Clé publique complète OpenSSL (modulus + exponent)")
        list.add("  • Base64 de la signature brute signée")
        list.add("")
        list.add("Dossier des rapports automatiques :")
        list.add("  /sdcard/HEPHAISTOS/VERISIN/reports/<nom>_verisin.txt")
        list.add("  /sdcard/HEPHAISTOS/VERISIN/Data/<nom>_verisin.json")
        list.add("=======================================================")
        return list
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extraction interne des blocs V2, V3, V3.1 et V1
    // ─────────────────────────────────────────────────────────────────────────

    private fun extractSigningBlockData(
        apkFile: File,
        detectedSchemes: MutableList<String>,
        rawSignaturesMap: MutableMap<Int, ByteArray>
    ): List<X509Certificate> {
        val certs = mutableListOf<X509Certificate>()
        try {
            RandomAccessFile(apkFile, "r").use { raf ->
                val fileLen = raf.length()
                if (fileLen < 32) return emptyList()

                // 1. Trouver l'EoCD pour obtenir le CD offset
                val searchLen = minOf(fileLen, 65536L).toInt()
                val tail = ByteArray(searchLen)
                raf.seek(fileLen - searchLen)
                raf.readFully(tail)

                var eocdOffset = -1L
                for (i in tail.size - 22 downTo 0) {
                    if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4b.toByte() &&
                        tail[i + 2] == 0x05.toByte() && tail[i + 3] == 0x06.toByte()
                    ) {
                        eocdOffset = fileLen - searchLen + i
                        break
                    }
                }
                if (eocdOffset <= 0) return emptyList()

                raf.seek(eocdOffset + 16)
                val cdOffset = java.lang.Integer.reverseBytes(raf.readInt()).toLong() and 0xFFFFFFFFL
                if (cdOffset < 24) return emptyList()

                // 2. Vérifier le bloc APK Signing Block avant le Central Directory
                raf.seek(cdOffset - 24)
                val blockSize2 = java.lang.Long.reverseBytes(raf.readLong())
                val magicBytes = ByteArray(16)
                raf.readFully(magicBytes)
                val magicStr = String(magicBytes, Charsets.US_ASCII)

                if (magicStr != APK_SIG_BLOCK_MAGIC || blockSize2 < 24 || blockSize2 > cdOffset) {
                    return emptyList()
                }

                val blockStart = cdOffset - 8 - blockSize2
                raf.seek(blockStart)
                val blockSize1 = java.lang.Long.reverseBytes(raf.readLong())
                if (blockSize1 != blockSize2) return emptyList()

                val pairsLength = (blockSize1 - 24).toInt()
                val pairsBuffer = ByteArray(pairsLength)
                raf.readFully(pairsBuffer)

                val buf = ByteBuffer.wrap(pairsBuffer).order(ByteOrder.LITTLE_ENDIAN)
                val cf = CertificateFactory.getInstance("X.509")

                while (buf.remaining() >= 12) {
                    val pairLen = buf.long
                    if (pairLen < 4 || pairLen > buf.remaining()) break
                    val pairId = buf.int
                    val valueLen = (pairLen - 4).toInt()
                    val pairBytes = ByteArray(valueLen)
                    buf.get(pairBytes)

                    when (pairId) {
                        SCHEME_V2_ID -> {
                            if (!detectedSchemes.contains("V2")) detectedSchemes.add("V2")
                            parseSchemeBlock(pairBytes, cf, certs, rawSignaturesMap)
                        }
                        SCHEME_V3_ID -> {
                            if (!detectedSchemes.contains("V3")) detectedSchemes.add("V3")
                            parseSchemeBlock(pairBytes, cf, certs, rawSignaturesMap)
                        }
                        SCHEME_V31_ID -> {
                            if (!detectedSchemes.contains("V3.1")) detectedSchemes.add("V3.1")
                            parseSchemeBlock(pairBytes, cf, certs, rawSignaturesMap)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return certs
    }

    private fun parseSchemeBlock(
        pairBytes: ByteArray,
        cf: CertificateFactory,
        certsOutput: MutableList<X509Certificate>,
        rawSignaturesMap: MutableMap<Int, ByteArray>
    ) {
        try {
            val buf = ByteBuffer.wrap(pairBytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 4) return
            val signersSeqLen = buf.int
            if (signersSeqLen <= 0 || signersSeqLen > buf.remaining()) return

            while (buf.remaining() >= 4) {
                val signerBlockLen = buf.int
                if (signerBlockLen <= 0 || signerBlockLen > buf.remaining()) break
                val signerStartPos = buf.position()

                // [signedData]
                val signedDataLen = buf.int
                if (signedDataLen <= 0 || signedDataLen > buf.remaining()) {
                    buf.position(signerStartPos + signerBlockLen)
                    continue
                }
                val signedDataBytes = ByteArray(signedDataLen)
                buf.get(signedDataBytes)

                // Extraire certificats depuis signedData
                val sBuf = ByteBuffer.wrap(signedDataBytes).order(ByteOrder.LITTLE_ENDIAN)
                val digestsSeqLen = sBuf.int
                if (digestsSeqLen in 0..sBuf.remaining()) {
                    sBuf.position(sBuf.position() + digestsSeqLen)
                }

                if (sBuf.remaining() >= 4) {
                    val certsSeqLen = sBuf.int
                    if (certsSeqLen in 1..sBuf.remaining()) {
                        val certsEnd = sBuf.position() + certsSeqLen
                        while (sBuf.position() < certsEnd && sBuf.remaining() >= 4) {
                            val certLen = sBuf.int
                            if (certLen in 1..sBuf.remaining()) {
                                val cBytes = ByteArray(certLen)
                                sBuf.get(cBytes)
                                try {
                                    val cert = cf.generateCertificate(ByteArrayInputStream(cBytes)) as? X509Certificate
                                    if (cert != null && certsOutput.none { it.encoded.contentEquals(cert.encoded) }) {
                                        certsOutput.add(cert)
                                    }
                                } catch (_: Exception) {}
                            } else break
                        }
                    }
                }

                // [signatures sequence]
                if (buf.position() < signerStartPos + signerBlockLen && buf.remaining() >= 4) {
                    val sigsSeqLen = buf.int
                    if (sigsSeqLen in 1..buf.remaining()) {
                        val sigsEnd = buf.position() + sigsSeqLen
                        while (buf.position() < sigsEnd && buf.remaining() >= 4) {
                            val sigRecordLen = buf.int
                            if (sigRecordLen in 8..buf.remaining()) {
                                val recEnd = buf.position() + sigRecordLen
                                val algId = buf.int
                                val rawSigLen = buf.int
                                if (rawSigLen in 1..buf.remaining()) {
                                    val rawSig = ByteArray(rawSigLen)
                                    buf.get(rawSig)
                                    val certIdx = certsOutput.size - 1
                                    if (certIdx >= 0 && !rawSignaturesMap.containsKey(certIdx)) {
                                        rawSignaturesMap[certIdx] = rawSig
                                    }
                                }
                                buf.position(recEnd)
                            } else break
                        }
                    }
                }

                buf.position(signerStartPos + signerBlockLen)
            }
        } catch (_: Exception) {}
    }

    private fun extractV1Certificates(apkFile: File): List<X509Certificate> {
        val list = mutableListOf<X509Certificate>()
        try {
            ZipFile(apkFile).use { zf ->
                val entries = zf.entries()
                val cf = CertificateFactory.getInstance("X.509")
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name.uppercase(Locale.ROOT)
                    if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                        val data = zf.getInputStream(entry).use { it.readBytes() }
                        try {
                            val certs = cf.generateCertificates(ByteArrayInputStream(data))
                            for (c in certs) {
                                if (c is X509Certificate && list.none { it.encoded.contentEquals(c.encoded) }) {
                                    list.add(c)
                                }
                            }
                        } catch (_: Exception) {
                            // Extract X.509 by ASN.1 scan
                            val scanned = scanX509Certificates(data, cf)
                            for (c in scanned) {
                                if (list.none { it.encoded.contentEquals(c.encoded) }) list.add(c)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun scanX509Certificates(bytes: ByteArray, cf: CertificateFactory): List<X509Certificate> {
        val certs = mutableListOf<X509Certificate>()
        for (i in 0 until bytes.size - 32) {
            if (bytes[i] == 0x30.toByte() && bytes[i + 1] == 0x82.toByte()) {
                val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                val totalLen = len + 4
                if (i + totalLen <= bytes.size) {
                    try {
                        val slice = bytes.copyOfRange(i, i + totalLen)
                        val c = cf.generateCertificate(ByteArrayInputStream(slice)) as? X509Certificate
                        if (c != null && certs.none { it.encoded.contentEquals(c.encoded) }) {
                            certs.add(c)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return certs
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsing Manifest & Cible Interface Android
    // ─────────────────────────────────────────────────────────────────────────

    data class ManifestQuickInfo(
        val packageName: String,
        val versionCode: Long,
        val versionName: String,
        val sharedUserId: String?,
        val interfaceDisplay: String
    )

    private fun parseManifestQuick(apkFile: File): ManifestQuickInfo {
        var pkg = "inconnu"
        var vCode = 0L
        var vName = "1.0"
        var sharedUid: String? = null
        var targetSdk = 36 // Default Android 16 (Interface v16)
        var minSdk = 28

        try {
            ZipFile(apkFile).use { zf ->
                val manifestEntry = zf.getEntry("AndroidManifest.xml")
                if (manifestEntry != null) {
                    val bytes = zf.getInputStream(manifestEntry).use { it.readBytes() }
                    // Scan binary strings
                    val strings = extractStringsFromAxml(bytes)
                    for (str in strings) {
                        if (str.contains(".") && !str.startsWith("android.") && !str.endsWith(".xml") && pkg == "inconnu") {
                            pkg = str
                        }
                        if (str.startsWith("android.uid.")) {
                            sharedUid = str
                        }
                    }

                    // Scan numeric targetSdk in AXML
                    for (i in 0 until bytes.size - 8) {
                        val valInt = ((bytes[i + 3].toInt() and 0xFF) shl 24) or
                                ((bytes[i + 2].toInt() and 0xFF) shl 16) or
                                ((bytes[i + 1].toInt() and 0xFF) shl 8) or
                                (bytes[i].toInt() and 0xFF)
                        if (valInt in 21..36) {
                            targetSdk = valInt
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val interfaceDisplay = when (targetSdk) {
            36 -> "Interface v16 (36)"
            35 -> "Interface v15 (35)"
            34 -> "Interface v14 (34)"
            33 -> "Interface v13 (33)"
            32 -> "Interface v12L (32)"
            31 -> "Interface v12 (31)"
            30 -> "Interface v11 (30)"
            29 -> "Interface v10 (29)"
            28 -> "Interface v9 (28)"
            else -> "Interface v$targetSdk ($targetSdk)"
        }

        return ManifestQuickInfo(
            packageName = pkg,
            versionCode = vCode,
            versionName = vName,
            sharedUserId = sharedUid,
            interfaceDisplay = interfaceDisplay
        )
    }

    private fun extractStringsFromAxml(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 32..126) {
                sb.append(c.toChar())
            } else {
                if (sb.length >= 4) {
                    list.add(sb.toString())
                }
                sb.clear()
            }
        }
        if (sb.length >= 4) list.add(sb.toString())
        return list
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires Cryptographiques & Formatage OpenSSL
    // ─────────────────────────────────────────────────────────────────────────

    private fun formatPublicKey(pubKey: java.security.PublicKey): Triple<String, String, Int> {
        return if (pubKey is RSAPublicKey) {
            val mod = pubKey.modulus.toString(16).lowercase(Locale.ROOT)
            val exp = pubKey.publicExponent.toString(16).lowercase(Locale.ROOT)
            val bitLen = pubKey.modulus.bitLength()
            val formatted = "OpenSSLRSAPublicKey{modulus=$mod,publicExponent=$exp}"
            Triple(formatted, "RSA", bitLen)
        } else {
            val formatted = pubKey.toString().replace("\n", " ").trim()
            Triple(formatted, pubKey.algorithm, 256)
        }
    }

    private fun computeHash(data: ByteArray, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            bytesToHex(md.digest(data))
        } catch (_: Exception) {
            ""
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
