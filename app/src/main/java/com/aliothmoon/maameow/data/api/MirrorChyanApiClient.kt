package com.aliothmoon.maameow.data.api

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.api.model.MirrorChyanData
import com.aliothmoon.maameow.data.api.model.MirrorChyanResponse
import com.aliothmoon.maameow.data.model.update.UpdateError
import com.aliothmoon.maameow.utils.i18n.LocalizedException
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import com.aliothmoon.maameow.utils.JsonUtils
import timber.log.Timber

/**
 * MirrorChyan API 通用客户端
 * 封装 HTTP 请求、响应解析和业务错误映射
 */
class MirrorChyanApiClient(private val httpClient: HttpClientHelper) {

    private val json = JsonUtils.common

    /**
     * 调用 MirrorChyan latest API
     * @throws MirrorChyanBizException 业务错误（CDK 无效、资源不存在等）
     */
    suspend fun getLatest(
        api: String,
        query: Map<String, String>,
        fetchVersion: Boolean = false
    ): Result<MirrorChyanData> {
        return runCatching {
            val response = httpClient.get(api, query = query)

            response.use { resp ->
                if (resp.code == 500) {
                    // 消息体留空：UpdateError.fromCode(500) 固定映射到
                    // R.string.update_error_service_unavailable，这里传的串永远不会展示
                    throw MirrorChyanBizException(500, null)
                }

                val body = kotlin.runCatching {
                    json.decodeFromString<MirrorChyanResponse>(resp.body.string())
                }.getOrDefault(MirrorChyanResponse.UNKNOWN_ERR)

                if (body.code != 0 && !fetchVersion) {
                    throw MirrorChyanBizException(body.code, body.msg)
                }

                // 同上：fromCode(-1) 固定映射到 R.string.update_error_empty_data
                body.data ?: throw MirrorChyanBizException(-1, null)
            }
        }.onFailure { e ->
            Timber.e(e, "MirrorChyan API 请求失败: $api")
        }
    }
}

/** MirrorChyan 业务异常，携带业务错误码 */
class MirrorChyanBizException(val bizCode: Int, override val message: String?) :
    Exception(message) {
    fun toUpdateError(): UpdateError = UpdateError.fromCode(bizCode, message)
}

/** CDK 未配置异常 */
class CdkRequiredException :
    LocalizedException(uiTextOf(R.string.update_error_cdk_required))
