package com.lagradost.cloudstream3.animeproviders

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import java.util.*


class LatAnimeProvider : MainAPI() {
    companion object {
        fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Especial")) TvType.OVA
            else if (t.contains("Pelicula")) TvType.AnimeMovie
            else TvType.Anime
        }

        fun getDubStatus(title: String): DubStatus {
            return if (title.contains("Latino") || title.contains("Castellano"))
                DubStatus.Dubbed
            else DubStatus.Subbed
        }
    }

    override var mainUrl = "https://latanime.org"
    override var name = "LatAnime"
    override var lang = "es"
    override val hasMainPage = false //true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.OVA,
        TvType.Anime,
    )

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/buscar?q=$query", timeout = 120).document.select(".col-6.my-3").map { //.col-6
            val title = it.selectFirst(".my-1")!!.text() //.setistitles
            val href = fixUrl(it.selectFirst("a")!!.attr("href"))
            val image = it.selectFirst("img.img-fluid2.shadow-sm")!!.attr("src") //img.animemainimg
            AnimeSearchResponse(
                title,
                href,
                this.name,
                TvType.Anime,
                fixUrl(image),
                null,
                if (title.contains("Latino") || title.contains("Castellano")) EnumSet.of(
                    DubStatus.Dubbed
                ) else EnumSet.of(DubStatus.Subbed),
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, timeout = 120).document
        val poster = doc.selectFirst(".chapterpic img")!!.attr("src")
        val backimage = doc.selectFirst("div.heroarea div.heromain div.herobg img")!!.attr("src")
        val title = doc.selectFirst(".chapterdetails h1")!!.text()
        val type = doc.selectFirst("div.chapterdetls2")?.text() ?: ""
        val description = doc.selectFirst("p.textComplete")!!.text().replace("Ver menos", "")
        val genres = doc.select(".breadcrumb-item a").map { it.text() }
        val status = when (doc.selectFirst("button.btn1")?.text()) {
            "Estreno" -> ShowStatus.Ongoing
            "Finalizado" -> ShowStatus.Completed
            else -> null
        }
        val episodes = doc.select("div.col-item").map {
            val name = it.selectFirst("p.animetitles")!!.text()
            val link = it.selectFirst("a")!!.attr("href")
            val epThumb = it.selectFirst(".animeimghv")?.attr("data-src") ?: it.selectFirst("div.animeimgdiv img.animeimghv")?.attr("src")
            Episode(link, name, posterUrl = epThumb)
        }
        return newAnimeLoadResponse(title, url, getType(type)) {
            posterUrl = poster
            backgroundPosterUrl = backimage
            addEpisodes(DubStatus.Subbed, episodes)
            showStatus = status
            plot = description
            tags = genres
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        app.get(data).document.select("div.playother p").apmap {
            val encodedurl = it.select("p").attr("data-player")
            val urlDecoded = base64Decode(encodedurl)
            val url = (urlDecoded).replace("https://latanime.org/reproductor?url=", "")
            loadExtractor(url, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
 