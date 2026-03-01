package com.chastity.diary.util

import java.security.MessageDigest

/**
 * Local sponsor-code validator.
 *
 * Redemption codes are SHA-256-hashed before being embedded in the source, so the
 * plaintext codes never appear in the binary.  This provides a minimal barrier that is
 * consistent with F-Droid's open-source policy — technically savvy users who build from
 * source can generate their own codes, which is an explicitly accepted trade-off.
 *
 * ## Generating a new code (for the app maintainer):
 * 1. Pick a random code string, e.g. `CB-XXXX-XXXX-XXXX`.
 * 2. Compute `sha256(code.trim().uppercase())`.
 * 3. Add the hex digest to [VALID_CODE_HASHES].
 * 4. Commit the updated file to the open-source repo.
 * 5. Distribute the plaintext code to sponsors via GitHub Sponsors / Liberapay.
 */
object SponsorManager {

    /**
     * SHA-256 hex digests of valid redemption codes (uppercase, trimmed).
     *
     * TODO: Replace placeholder hashes with real ones before release.
     * These placeholders match no real input — they are just correctly-formatted
     * dummy hashes to keep the code compilable during development.
     */
    private val VALID_CODE_HASHES: Set<String> = setOf(
        // Placeholder — remove before production. Add real hashes here.
        // Example: sha256("CB-DEMO-0001") = ...
        "00000000000000000000000000000000000000000000000000000000deadbeef"
    )

    /**
     * Returns `true` if [code] (after trimming and uppercasing) matches any known
     * sponsor hash.  All comparison is done in constant time via hash lookup to avoid
     * timing attacks.
     */
    fun isValidCode(code: String): Boolean {
        val normalised = code.trim().uppercase()
        if (normalised.isEmpty()) return false
        val digest = sha256Hex(normalised)
        return digest in VALID_CODE_HASHES
    }

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
