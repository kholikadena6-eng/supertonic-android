package com.brahmadeo.supertonic.tts.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.publication.services.content.content
import java.io.File

class EbookParser(private val context: Context) {

    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
    private val publicationOpener = PublicationOpener(publicationParser)

    suspend fun parseUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = uri.toAbsoluteUrl()
                ?: return@withContext Result.failure<String>(Exception("Failed to convert URI to Readium URL"))

            val asset = assetRetriever.retrieve(url).getOrElse { error: Error ->
                return@withContext Result.failure<String>(Exception("Failed to retrieve asset: ${error.message}"))
            }

            val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error: Error ->
                return@withContext Result.failure<String>(Exception("Failed to open publication: ${error.message}"))
            }

            val content = publication.content()
            if (content == null) {
                return@withContext Result.failure<String>(Exception("This publication does not support content extraction."))
            }

            val text = content.text()
            if (text == null || text.isBlank()) {
                return@withContext Result.failure<String>(Exception("No text content could be extracted from this publication."))
            }

            Result.success(text)
        } catch (e: Exception) {
            Result.failure<String>(e)
        }
    }
}
