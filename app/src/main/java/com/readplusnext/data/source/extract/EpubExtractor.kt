package com.readplusnext.data.source.extract

import com.readplusnext.core.util.NaturalOrderComparator
import com.readplusnext.data.source.ComicImportException
import com.readplusnext.data.source.ImageFormats
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Reads an EPUB through `META-INF/container.xml` -> OPF manifest/spine, so pages follow the
 * reading order instead of the archive order. XHTML spine items are scanned for images.
 */
internal object EpubExtractor {

    fun extract(epub: File, outputDir: File): List<File> {
        val orderedEntries = mutableListOf<String>()
        ZipFile(epub).use { zip ->
            val opfPath = resolveOpfPath(zip)
            if (opfPath != null) {
                collectSpineImages(zip, opfPath, orderedEntries)
            }
            if (orderedEntries.isEmpty()) {
                zip.entries()
                    .asSequence()
                    .filter { !it.isDirectory && ImageFormats.isImageName(it.name) }
                    .map { it.name }
                    .sortedWith(NaturalOrderComparator)
                    .forEach { orderedEntries += it }
            }

            val written = mutableListOf<File>()
            orderedEntries.distinct().forEachIndexed { index, entryName ->
                val entry = zip.getEntry(entryName) ?: return@forEachIndexed
                val extension = ImageFormats.extensionFor(entryName)
                val target = File(outputDir, "%05d.%s".format(Locale.US, index, extension))
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                written += target
            }
            if (written.isEmpty()) throw ComicImportException("EPUB 中未找到可显示的图片")
            return written
        }
    }

    private fun resolveOpfPath(zip: ZipFile): String? {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
        val document = parseXml(zip.getInputStream(containerEntry)) ?: return null
        val rootFiles = document.getElementsByTagName("rootfile")
        for (index in 0 until rootFiles.length) {
            val path = (rootFiles.item(index) as? Element)?.getAttribute("full-path")
            if (!path.isNullOrBlank()) return decode(path)
        }
        return null
    }

    private fun collectSpineImages(zip: ZipFile, opfPath: String, ordered: MutableList<String>) {
        val opfEntry = zip.getEntry(opfPath) ?: return
        val opf = parseXml(zip.getInputStream(opfEntry)) ?: return

        val manifest = mutableMapOf<String, String>()
        val items = opf.getElementsByTagName("item")
        for (index in 0 until items.length) {
            val item = items.item(index) as? Element ?: continue
            val id = item.getAttribute("id")
            val href = item.getAttribute("href")
            if (id.isNotEmpty() && href.isNotEmpty()) manifest[id] = href
        }

        val opfDir = opfPath.substringBeforeLast('/', "")
        val spine = opf.getElementsByTagName("itemref")
        for (index in 0 until spine.length) {
            val idref = (spine.item(index) as? Element)?.getAttribute("idref") ?: continue
            val href = manifest[idref] ?: continue
            val path = resolve(opfDir, href)
            collectImagesFrom(zip, path, ordered)
        }
    }

    private fun collectImagesFrom(zip: ZipFile, path: String, ordered: MutableList<String>) {
        val entry = zip.getEntry(path) ?: return
        if (ImageFormats.isImageName(path)) {
            ordered += path
            return
        }
        val document = parseXml(zip.getInputStream(entry)) ?: return
        val baseDir = path.substringBeforeLast('/', "")

        val images = document.getElementsByTagName("img")
        for (index in 0 until images.length) {
            val source = (images.item(index) as? Element)?.getAttribute("src") ?: continue
            addIfImage(zip, baseDir, source, ordered)
        }

        val svgImages = document.getElementsByTagName("image")
        for (index in 0 until svgImages.length) {
            val element = svgImages.item(index) as? Element ?: continue
            val href = element.getAttribute("xlink:href").ifEmpty { element.getAttribute("href") }
            addIfImage(zip, baseDir, href, ordered)
        }
    }

    private fun addIfImage(zip: ZipFile, baseDir: String, rawHref: String, ordered: MutableList<String>) {
        val href = rawHref.substringBefore('#').trim()
        if (href.isEmpty() || href.startsWith("data:")) return
        val path = resolve(baseDir, href)
        if (ImageFormats.isImageName(path) && zip.getEntry(path) != null) ordered += path
    }

    private fun resolve(baseDir: String, href: String): String {
        val segments = ArrayDeque<String>()
        baseDir.split('/').filter { it.isNotEmpty() }.forEach { segments.addLast(it) }
        decode(href).split('/').filter { it.isNotEmpty() }.forEach { part ->
            when (part) {
                "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeLast()
                else -> segments.addLast(part)
            }
        }
        return segments.joinToString("/")
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun parseXml(input: InputStream): Document? = try {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        runCatching {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        input.use { factory.newDocumentBuilder().parse(it) }
    } catch (error: Exception) {
        null
    }
}
