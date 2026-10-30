package com.runehearth.app.net

import com.runehearth.app.engine.Action
import com.runehearth.app.engine.ActionResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

const val PROTOCOL_VERSION = 1
const val SERVICE_TYPE = "_runehearth._tcp."

@Serializable
data class WireChange(val type: String, val id: String, val seq: Long, val payload: String = "", val delete: Boolean = false)

@Serializable
data class Claimable(val id: String, val name: String, val summary: String)

/** Protocol messages. Pre-authentication messages travel in plain frames; everything after `Auth` is HMAC'd. */
@Serializable
sealed interface Msg {
    // ---- handshake (plain)
    @Serializable @SerialName("challenge") data class Challenge(val proto: Int, val nonce: String, val campaignId: String, val campaignName: String, val epoch: Int) : Msg
    @Serializable @SerialName("auth") data class Auth(
        val deviceId: String, val name: String, val nonce: String, val proof: String,
        val lastSeq: Long, val epoch: Int, val claimId: String? = null, val newName: String? = null,
        /** `player` (default) or `dm`: a Dungeon Master controller device that does not host. */
        val role: String = "player",
    ) : Msg
    @Serializable @SerialName("denied") data class Denied(val reason: String) : Msg

    // ---- session (authenticated)
    @Serializable @SerialName("needClaim") data class NeedClaim(val claimable: List<Claimable>) : Msg
    @Serializable @SerialName("claim") data class Claim(val claimId: String? = null, val newName: String? = null) : Msg
    @Serializable @SerialName("bound") data class Bound(val characterId: String, val hostSeq: Long, val epoch: Int) : Msg
    @Serializable @SerialName("delta") data class Delta(val changes: List<WireChange>, val toSeq: Long, val full: Boolean = false) : Msg
    @Serializable @SerialName("act") data class Act(val action: Action) : Msg
    @Serializable @SerialName("actResult") data class ActResult(val result: ActionResult) : Msg
    @Serializable @SerialName("digest") data class DigestReport(val seq: Long, val digest: Long) : Msg
    /** DM controller: the session is bound; a full snapshot (in parts) follows. */
    @Serializable @SerialName("dmBound") data class DmBound(val hostSeq: Long, val epoch: Int) : Msg
    @Serializable @SerialName("snapPart") data class SnapshotPart(val index: Int, val total: Int, val data: String) : Msg
    /** DM controller incremental update: full-fidelity entity payloads, regardless of player visibility. */
    @Serializable @SerialName("dmDelta") data class DmDelta(val changes: List<WireChange>, val toSeq: Long) : Msg
    @Serializable @SerialName("ping") data class Ping(val t: Long, val rtt: Long = -1) : Msg
    @Serializable @SerialName("pong") data class Pong(val t: Long) : Msg
    @Serializable @SerialName("kick") data class Kick(val reason: String) : Msg
}

object WireJson {
    val json = Json { encodeDefaults = false; ignoreUnknownKeys = true; classDiscriminator = "type" }
    fun encode(m: Msg): ByteArray = json.encodeToString(Msg.serializer(), m).toByteArray(Charsets.UTF_8)
    fun decode(b: ByteArray): Msg = json.decodeFromString(Msg.serializer(), String(b, Charsets.UTF_8))
}

/**
 * Length-prefixed frames: `[flag:1][len:4][payload][mac:16 if flag==1]`.
 * `mac = HMAC-SHA256(key, counter:8 ‖ payload)[0..16]`; the per-direction counter stops replay and reordering.
 */
object Frames {
    const val MAX_FRAME = 1 shl 20
    private const val MAC_LEN = 16

    private fun mac(key: ByteArray, counter: Long, payload: ByteArray): ByteArray {
        val m = Mac.getInstance("HmacSHA256")
        m.init(SecretKeySpec(key, "HmacSHA256"))
        m.update(java.nio.ByteBuffer.allocate(8).putLong(counter).array())
        m.update(payload)
        return m.doFinal().copyOf(MAC_LEN)
    }

    fun write(out: DataOutputStream, payload: ByteArray, key: ByteArray?, counter: Long) {
        require(payload.size <= MAX_FRAME) { "frame too large" }
        out.writeByte(if (key == null) 0 else 1)
        out.writeInt(payload.size)
        out.write(payload)
        if (key != null) out.write(mac(key, counter, payload))
        out.flush()
    }

    class Frame(val payload: ByteArray, val authenticated: Boolean)

    /**
     * Reads one frame. With a non-null [key] MAC'd frames are verified; a plain frame is still returned (marked
     * unauthenticated) so a client can receive a plain `Denied`; the caller must reject any other plain message.
     */
    fun read(inp: DataInputStream, key: ByteArray?, counter: Long): Frame {
        val flag = inp.readUnsignedByte()
        val len = inp.readInt()
        if (len < 0 || len > MAX_FRAME) throw IOException("bad frame length $len")
        if (flag == 1 && key == null) throw IOException("unexpected authenticated frame")
        val payload = ByteArray(len)
        inp.readFully(payload)
        if (flag == 1) {
            val got = ByteArray(MAC_LEN)
            inp.readFully(got)
            if (!MessageDigest.isEqual(got, mac(key!!, counter, payload))) throw IOException("bad frame MAC")
        }
        return Frame(payload, flag == 1)
    }
}

/** Pairing-code crypto: challenge-response proof and per-session key derivation. The code itself is never sent. */
object Pairing {
    private fun hmac(key: ByteArray, vararg parts: ByteArray): ByteArray {
        val m = Mac.getInstance("HmacSHA256")
        m.init(SecretKeySpec(key, "HmacSHA256"))
        parts.forEach { m.update(it) }
        return m.doFinal()
    }

    fun proof(code: String, hostNonce: String, clientNonce: String, deviceId: String): String =
        hex(hmac(code.toByteArray(), "proof".toByteArray(), hostNonce.toByteArray(), clientNonce.toByteArray(), deviceId.toByteArray()))

    fun sessionKey(code: String, hostNonce: String, clientNonce: String): ByteArray =
        hmac(code.toByteArray(), "key".toByteArray(), hostNonce.toByteArray(), clientNonce.toByteArray())

    fun verify(expected: String, got: String): Boolean = MessageDigest.isEqual(expected.toByteArray(), got.toByteArray())

    fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }
}
