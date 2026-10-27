package com.example.data

import android.content.Context
import android.util.Base64
import com.example.model.KeycoreTargetAnalysis
import com.example.model.KeysetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KEYCORE Engine — Moteur cryptographique de création, réplication et forge de clés.
 * Partie intégrante de la suite SCEAU dans HephaistOS.
 */
object KeycoreEngine {

    /**
     * 1. KEYMAKER : Forge standard de trousseau de clés AOSP / Android
     */
    suspend fun generateStandardKeymakerKeyset(
        context: Context,
        keysetName: String,
        commonName: String,
        organization: String,
        countryCode: String,
        algorithm: String,
        validityYears: Int,
        onProgress: (step: String, percent: Float) -> Unit = { _, _ -> },
        onLog: (String) -> Unit = {}
    ): KeysetInfo = withContext(Dispatchers.IO) {
        val sanitizedName = keysetName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "keyset_${System.currentTimeMillis()}" }
        
        // Save in SCEAU/KEYCORE/
        val keycoreBaseDir = WorkspaceManager.getKeycoreDir(context)
        val targetDir = File(keycoreBaseDir, sanitizedName).apply { if (!exists()) mkdirs() }

        // Also mirror in SIGNER/keysets/ for GÉMEAU integration
        val signerBaseDir = WorkspaceManager.getSignerKeysetDir(context)
        val signerDir = File(signerBaseDir, sanitizedName).apply { if (!exists()) mkdirs() }

        onLog("[KEYCORE:KEYMAKER] Forge du trousseau '$sanitizedName'...")
        onLog("Algorithme : $algorithm • Validité : $validityYears ans")
        onLog("Sujet : CN=$commonName, O=$organization, C=$countryCode")

        val keysetInfo = GemeauSignerEngine.generateKeyset(
            context = context,
            keysetName = sanitizedName,
            commonName = commonName,
            organization = organization,
            countryCode = countryCode,
            algorithm = algorithm,
            validityYears = validityYears,
            onProgress = onProgress,
            onLog = onLog
        )

        // Copy files from signerDir to targetDir
        try {
            signerDir.listFiles()?.forEach { file ->
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }
        } catch (_: Exception) {}

        keysetInfo.copy(dirPath = targetDir.absolutePath)
    }

    /**
     * Analyse un APK cible avec Verisin pour alimenter Verisin Key et Verisin Clone
     */
    suspend fun analyzeApkForTarget(
        context: Context,
        apkFile: File,
        onLog: (String) -> Unit = {}
    ): KeycoreTargetAnalysis = withContext(Dispatchers.IO) {
        onLog("[KEYCORE] Analyse de signature avec VERISIN sur ${apkFile.name}...")
        val verisinResult = VerisinEngine.verifyApkFile(context, apkFile, onLog)

        val firstCert = verisinResult.certificates.firstOrNull()
        val keyType = firstCert?.keyType ?: "RSA"
        val keySize = firstCert?.keySizeBits ?: 2048
        val sigAlg = firstCert?.sigAlgName ?: "SHA256withRSA"
        val subject = firstCert?.subjectDn ?: "CN=Android,OU=Android,O=Android,C=US"
        val issuer = firstCert?.issuerDn ?: subject
        val isSelf = firstCert?.isSelfSigned ?: true

        KeycoreTargetAnalysis(
            apkPath = apkFile.absolutePath,
            apkName = apkFile.name,
            schemesDisplay = verisinResult.schemesDisplay,
            keyType = keyType,
            keySizeBits = keySize,
            sigAlgName = sigAlg,
            subjectDn = subject,
            issuerDn = issuer,
            validityYears = 25,
            isSelfSigned = isSelf
        )
    }

    /**
     * 2. VERISIN KEY : Création d'une clé basée sur la typologie de l'APK cible
     * avec de nouvelles identités DN personnalisées.
     */
    suspend fun createVerisinKey(
        context: Context,
        targetAnalysis: KeycoreTargetAnalysis,
        keysetName: String,
        commonName: String,
        organization: String,
        countryCode: String,
        validityYears: Int,
        onLog: (String) -> Unit = {}
    ): KeysetInfo = withContext(Dispatchers.IO) {
        val sanitizedName = keysetName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "verisin_key_${System.currentTimeMillis()}" }
        val outDir = File(WorkspaceManager.getKeycoreDir(context), sanitizedName).apply { if (!exists()) mkdirs() }
        val signerMirrorDir = File(WorkspaceManager.getSignerKeysetDir(context), sanitizedName).apply { if (!exists()) mkdirs() }

        onLog("[KEYCORE:VERISIN KEY] Création d'une clé calquée sur '${targetAnalysis.apkName}'")
        onLog("Spécifications cibles : ${targetAnalysis.keyType} ${targetAnalysis.keySizeBits} bits • ${targetAnalysis.sigAlgName}")
        onLog("Nouvelle identité : CN=$commonName, O=$organization, C=$countryCode")

        val algoLabel = if (targetAnalysis.keyType.contains("EC", ignoreCase = true)) "ECDSA P-256" else "RSA ${targetAnalysis.keySizeBits} bits"

        val keyset = generateSingleOrFullKeyset(
            keysetName = sanitizedName,
            outDir = outDir,
            commonName = commonName,
            organization = organization,
            country = countryCode,
            keyType = targetAnalysis.keyType,
            keySize = targetAnalysis.keySizeBits,
            sigAlgName = targetAnalysis.sigAlgName,
            validityYears = validityYears,
            onLog = onLog
        )

        // Mirror to SIGNER/keysets for GÉMEAU and system signers
        try {
            outDir.listFiles()?.forEach { file ->
                file.copyTo(File(signerMirrorDir, file.name), overwrite = true)
            }
        } catch (_: Exception) {}

        keyset
    }

    /**
     * 3. VERISIN CLONE : Signature miroir / clone reproduisant fidèlement les DNs originaux
     * et l'architecture de la clé cible.
     */
    suspend fun createVerisinClone(
        context: Context,
        targetAnalysis: KeycoreTargetAnalysis,
        cloneKeysetName: String,
        onLog: (String) -> Unit = {}
    ): KeysetInfo = withContext(Dispatchers.IO) {
        val sanitizedName = cloneKeysetName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "clone_${targetAnalysis.apkName.removeSuffix(".apk")}" }
        val clonesDir = File(WorkspaceManager.getKeycoreClonesDir(context), sanitizedName).apply { if (!exists()) mkdirs() }
        val signerMirrorDir = File(WorkspaceManager.getSignerKeysetDir(context), sanitizedName).apply { if (!exists()) mkdirs() }

        onLog("=======================================================")
        onLog("[KEYCORE:VERISIN CLONE] Forge d'un Sceau Clone pour : ${targetAnalysis.apkName}")
        onLog("Reproduction du Sujet (DN) : ${targetAnalysis.subjectDn}")
        onLog("Reproduction de l'Algorithme : ${targetAnalysis.sigAlgName} (${targetAnalysis.keyType} ${targetAnalysis.keySizeBits} bits)")
        onLog("=======================================================")

        // Parse DN attributes if possible, or fallback cleanly
        val parsedDn = parseDnString(targetAnalysis.subjectDn)
        val commonName = parsedDn["CN"] ?: targetAnalysis.apkName.removeSuffix(".apk")
        val organization = parsedDn["O"] ?: "Android"
        val country = parsedDn["C"] ?: "US"

        val keyset = generateSingleOrFullKeyset(
            keysetName = sanitizedName,
            outDir = clonesDir,
            commonName = commonName,
            organization = organization,
            country = country,
            keyType = targetAnalysis.keyType,
            keySize = targetAnalysis.keySizeBits,
            sigAlgName = targetAnalysis.sigAlgName,
            validityYears = targetAnalysis.validityYears,
            customFullSubjectDn = targetAnalysis.subjectDn,
            onLog = onLog
        )

        // Mirror to SIGNER/keysets
        try {
            clonesDir.listFiles()?.forEach { file ->
                file.copyTo(File(signerMirrorDir, file.name), overwrite = true)
            }
        } catch (_: Exception) {}

        onLog("[SUCCÈS VERISIN CLONE] Sceau clone forgé dans : ${clonesDir.absolutePath}")
        keyset
    }

    /**
     * Resigne l'APK cible avec la clé clonée directement.
     */
    suspend fun resignApkWithCloneKey(
        context: Context,
        inputApk: File,
        cloneKeysetDir: File,
        onProgress: (String, Float) -> Unit = { _, _ -> },
        onLog: (String) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        onLog("[KEYCORE:RESIGN] Démarrage de la resignature clone de ${inputApk.name}...")
        onLog("Trousseau clone : ${cloneKeysetDir.name}")

        val signedList = GemeauSignerEngine.signMultipleApks(
            context = context,
            apks = listOf(inputApk),
            keysetDir = cloneKeysetDir,
            keyRole = "releasekey",
            onProgress = { step, pct, _ -> onProgress(step, pct) },
            onLog = onLog
        )

        val signedApk = signedList.firstOrNull()
        if (signedApk != null && signedApk.exists()) {
            // Also copy to cloneKeysetDir for easy retrieval
            val cloneOutputApk = File(cloneKeysetDir, "${inputApk.nameWithoutExtension}_clone_signed.apk")
            try {
                signedApk.copyTo(cloneOutputApk, overwrite = true)
            } catch (_: Exception) {}
            onLog("[RÉSULTAT] APK résigné avec succès : ${signedApk.absolutePath}")
            signedApk
        } else {
            onLog("[ERREUR] Échec de la resignature de ${inputApk.name}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal Crypto Routines
    // ─────────────────────────────────────────────────────────────────────────

    private fun generateSingleOrFullKeyset(
        keysetName: String,
        outDir: File,
        commonName: String,
        organization: String,
        country: String,
        keyType: String,
        keySize: Int,
        sigAlgName: String,
        validityYears: Int,
        customFullSubjectDn: String? = null,
        onLog: (String) -> Unit
    ): KeysetInfo {
        val isEc = keyType.contains("EC", ignoreCase = true)
        val roles = listOf("releasekey", "platform", "shared", "media", "networkstack", "avb")
        val certFingerprints = mutableMapOf<String, String>()

        for (role in roles) {
            val keyPair = if (isEc) {
                val kpg = KeyPairGenerator.getInstance("EC")
                kpg.initialize(256)
                kpg.generateKeyPair()
            } else {
                val kpg = KeyPairGenerator.getInstance("RSA")
                kpg.initialize(keySize)
                kpg.generateKeyPair()
            }

            // Write PK8 private key
            val pk8File = File(outDir, "$role.pk8")
            pk8File.writeBytes(keyPair.private.encoded)

            // Certificate
            val roleCommonName = if (customFullSubjectDn != null && role == "releasekey") commonName else "$commonName $role"
            val certBytes = createSelfSignedX509Certificate(
                keyPair = keyPair,
                commonName = roleCommonName,
                organization = organization,
                country = country,
                validityYears = validityYears,
                isEc = isEc,
                sigAlgName = sigAlgName
            )

            val pemFile = File(outDir, "$role.x509.pem")
            val certPem = buildString {
                append("-----BEGIN CERTIFICATE-----\n")
                append(Base64.encodeToString(certBytes, Base64.NO_WRAP).chunked(64).joinToString("\n"))
                append("\n-----END CERTIFICATE-----\n")
            }
            pemFile.writeText(certPem)

            val sha256 = MessageDigest.getInstance("SHA-256").digest(certBytes)
            val fpStr = sha256.joinToString(":") { "%02X".format(it) }
            certFingerprints[role] = fpStr
        }

        // AVB Pub
        val avbPubFile = File(outDir, "avb_pub.bin")
        val avbPem = File(outDir, "avb.x509.pem")
        if (avbPem.exists()) {
            avbPubFile.writeBytes(avbPem.readBytes())
        }

        // Keyset manifest
        val manifestFile = File(outDir, "keyset_manifest.json")
        val manifestJson = JSONObject().apply {
            put("keysetName", keysetName)
            put("commonName", commonName)
            put("organization", organization)
            put("country", country)
            put("algorithm", if (isEc) "ECDSA" else "RSA $keySize bits")
            put("validityYears", validityYears)
            put("creationDate", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH).format(Date()))
            if (customFullSubjectDn != null) put("customDn", customFullSubjectDn)
            val fpObj = JSONObject()
            certFingerprints.forEach { (k, v) -> fpObj.put(k, v) }
            put("fingerprints", fpObj)
            put("hasAvbKey", true)
        }
        manifestFile.writeText(manifestJson.toString(2))

        return KeysetInfo(
            name = keysetName,
            dirPath = outDir.absolutePath,
            keyCount = roles.size * 2,
            algorithm = if (isEc) "ECDSA P-256" else "RSA $keySize bits",
            creationDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(Date()),
            certFingerprints = certFingerprints,
            hasAvbKey = true
        )
    }

    private fun createSelfSignedX509Certificate(
        keyPair: KeyPair,
        commonName: String,
        organization: String,
        country: String,
        validityYears: Int,
        isEc: Boolean,
        sigAlgName: String
    ): ByteArray {
        val pubKeyBytes = keyPair.public.encoded
        val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter = Date(System.currentTimeMillis() + (validityYears * 365L * 86400000L))

        val dateFormat = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US)
        val notBeforeStr = dateFormat.format(notBefore)
        val notAfterStr = dateFormat.format(notAfter)

        // DN der sequence
        val dnDer = derSequence(
            derSet(derSequence(derOid("2.5.4.6"), derPrintableString(country.take(2).uppercase()))),
            derSet(derSequence(derOid("2.5.4.10"), derUtf8String(organization))),
            derSet(derSequence(derOid("2.5.4.3"), derUtf8String(commonName)))
        )

        val sigAlgOid = if (isEc) "1.2.840.10045.4.3.2" else "1.2.840.113549.1.1.11"
        val sigAlgDer = derSequence(derOid(sigAlgOid), derNull())

        val validityDer = derSequence(
            derUtcTime(notBeforeStr),
            derUtcTime(notAfterStr)
        )

        val tbsDer = derSequence(
            derExplicit(0, derInteger(BigInteger.valueOf(2))),
            derInteger(serialNumber),
            sigAlgDer,
            dnDer,
            validityDer,
            dnDer,
            pubKeyBytes
        )

        val signer = if (isEc) Signature.getInstance("SHA256withECDSA") else Signature.getInstance("SHA256withRSA")
        signer.initSign(keyPair.private)
        signer.update(tbsDer)
        val signatureBytes = signer.sign()

        return derSequence(
            tbsDer,
            sigAlgDer,
            derBitString(signatureBytes)
        )
    }

    private fun parseDnString(dn: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parts = dn.split(",")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2) {
                result[kv[0].trim().uppercase()] = kv[1].trim()
            }
        }
        return result
    }

    // DER Encoding helpers
    private fun derSequence(vararg elements: ByteArray): ByteArray {
        val totalLength = elements.sumOf { it.size }
        val out = ByteArrayOutputStream()
        out.write(0x30)
        writeDerLength(out, totalLength)
        elements.forEach { out.write(it) }
        return out.toByteArray()
    }

    private fun derSet(vararg elements: ByteArray): ByteArray {
        val totalLength = elements.sumOf { it.size }
        val out = ByteArrayOutputStream()
        out.write(0x31)
        writeDerLength(out, totalLength)
        elements.forEach { out.write(it) }
        return out.toByteArray()
    }

    private fun derInteger(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        val out = ByteArrayOutputStream()
        out.write(0x02)
        writeDerLength(out, bytes.size)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun derOid(oidStr: String): ByteArray {
        val parts = oidStr.split(".").map { it.toInt() }
        val out = ByteArrayOutputStream()
        out.write(0x06)
        val body = ByteArrayOutputStream()
        body.write(parts[0] * 40 + parts[1])
        for (i in 2 until parts.size) {
            var value = parts[i]
            val stack = mutableListOf<Int>()
            stack.add(value and 0x7F)
            value = value ushr 7
            while (value > 0) {
                stack.add((value and 0x7F) or 0x80)
                value = value ushr 7
            }
            stack.reversed().forEach { body.write(it) }
        }
        val b = body.toByteArray()
        writeDerLength(out, b.size)
        out.write(b)
        return out.toByteArray()
    }

    private fun derPrintableString(str: String): ByteArray {
        val bytes = str.toByteArray(Charsets.US_ASCII)
        val out = ByteArrayOutputStream()
        out.write(0x13)
        writeDerLength(out, bytes.size)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun derUtf8String(str: String): ByteArray {
        val bytes = str.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        out.write(0x0C)
        writeDerLength(out, bytes.size)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun derUtcTime(timeStr: String): ByteArray {
        val bytes = timeStr.toByteArray(Charsets.US_ASCII)
        val out = ByteArrayOutputStream()
        out.write(0x17)
        writeDerLength(out, bytes.size)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun derNull(): ByteArray = byteArrayOf(0x05, 0x00)

    private fun derBitString(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0x03)
        writeDerLength(out, bytes.size + 1)
        out.write(0x00)
        out.write(bytes)
        return out.toByteArray()
    }

    private fun derExplicit(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0xA0 or tag)
        writeDerLength(out, content.size)
        out.write(content)
        return out.toByteArray()
    }

    private fun writeDerLength(out: ByteArrayOutputStream, length: Int) {
        if (length < 128) {
            out.write(length)
        } else if (length < 256) {
            out.write(0x81)
            out.write(length)
        } else {
            out.write(0x82)
            out.write((length ushr 8) and 0xFF)
            out.write(length and 0xFF)
        }
    }
}
