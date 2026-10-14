package com.manunovel.app

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImportContacts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Cream = Color(0xFFF7F1E6)
private val Ink = Color(0xFF29261F)
private val Sage = Color(0xFF6E7861)
private val Sepia = Color(0xFFEAD9B8)
private val Dark = Color(0xFF1E211F)

data class Book(val id: String, val title: String, val author: String, val genre: String, val chapters: List<String>, val body: List<String>, val pdfUri: String? = null, val pdfName: String? = null)

private val samples = listOf(
    Book("lantern", "The Lantern at Willow Bend", "Mira Sen", "Mystery", listOf("The River Path", "A Light in the Rain"), listOf(
        "At the edge of Willow Bend stood a little house with blue shutters. Every evening, just as the sky turned the colour of a plum, a lantern appeared in its upstairs window. Nobody remembered seeing anyone carry it there.",
        "Anaya had lived beside the river all her life, and curiosity was stronger than fear. One rainy Thursday she followed the stepping-stones to the house. Beneath the door lay a dry envelope with her name written in careful green ink.",
        "Inside was a map of the river and a note: ‘When the water is quiet, listen.’ She waited until dawn. The river softened to glass, and from beneath the willow came the clear, bright sound of a tiny bell. Hidden in its roots was a tin box of letters, each one a promise made by the village long ago.",
        "Anaya carried the letters to the town hall. By sunset, neighbours who had barely spoken in years were sitting together, reading the old promises aloud. That evening the lantern glowed again—not as a mystery, but as a welcome.")),
    Book("postcards", "Postcards from Tomorrow", "Dev Arora", "Fantasy", listOf("A Strange Postmark", "The Last Message"), listOf(
        "The first postcard arrived on a Monday, tucked between a grocery bill and a school notice. Its picture showed the town square in spring, though winter frost still silvered the rooftops. On the back, in Nikhil’s own handwriting, were the words: ‘Plant the marigolds early.’",
        "He planted them in a cracked pot by the window. By May, a small gold forest nodded in the sun. The second postcard arrived with a picture of the old clock tower and a message about a missing key. The third showed his grandmother laughing beneath a paper crown.",
        "Each card offered no grand prophecy, only a small kindness he might choose to make. He returned a lost scarf, repaired a neighbour’s gate, and learned a new song for his grandmother. The town seemed to brighten, one ordinary day at a time.",
        "The final postcard arrived on his birthday. The picture showed Nikhil at his window, holding a stack of cards. The handwriting was his, but older and steadier: ‘Tomorrow is not a place you find. It is a place you make.’ He turned the card over. There was no next message—only a blank space, waiting."))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ManuNovelApp(contentResolver) } }
}

@Composable fun ManuNovelApp(resolver: ContentResolver) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("manu_novel_local", 0) }
    var imported by remember { mutableStateOf(loadImported(prefs)) }
    var progress by remember { mutableStateOf(loadProgress(prefs)) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var bookmarks by remember { mutableStateOf(prefs.getStringSet("bookmarks", emptySet())?.toSet() ?: emptySet()) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "Light") ?: "Light") }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("fontSize", 20f)) }
    var lineScale by remember { mutableFloatStateOf(prefs.getFloat("lineScale", 1.35f)) }
    var margin by remember { mutableFloatStateOf(prefs.getFloat("margin", 22f)) }
    var search by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<Book?>(null) }
    var reader by remember { mutableStateOf(false) }
    var pdfBook by remember { mutableStateOf<Book?>(null) }
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfPage by remember { mutableIntStateOf(0) }
    var pdfCount by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    val allBooks = remember(imported) { samples + imported }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = queryName(resolver, uri) ?: "Imported PDF"
                val b = Book("pdf_${uri.lastPathSegment ?: System.currentTimeMillis()}", name.removeSuffix(".pdf"), "PDF document", "PDF", emptyList(), emptyList(), uri.toString(), name)
                imported = imported.filterNot { it.pdfUri == uri.toString() } + b
                saveImported(prefs, imported); message = "PDF added to My Library"
            } catch (e: Exception) { message = "Could not import this PDF. Please choose a valid PDF file." }
        }
    }
    val bg = when(theme) { "Dark" -> Dark; "Sepia" -> Sepia; else -> Cream }
    val fg = if(theme == "Dark") Color(0xFFF3EEE4) else Ink
    MaterialTheme(colorScheme = lightColorScheme(background = bg, surface = bg, onSurface = fg, primary = Sage)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            if (reader && selected != null && selected!!.pdfUri == null) {
                ReaderScreen(selected!!, bg, fg, theme, { reader = false }, { t -> theme = t; prefs.edit().putString("theme", t).apply() }, fontSize, { fontSize = it.coerceIn(14f, 34f); prefs.edit().putFloat("fontSize", fontSize).apply() }, lineScale, { lineScale = it; prefs.edit().putFloat("lineScale", it).apply() }, margin, { margin = it; prefs.edit().putFloat("margin", it).apply() }, progress[selected!!.id] ?: 0, { p -> progress = progress + (selected!!.id to p); saveProgress(prefs, progress) }, bookmarks.contains(selected!!.id), { on -> bookmarks = if(on) bookmarks + selected!!.id else bookmarks - selected!!.id; prefs.edit().putStringSet("bookmarks", bookmarks).apply() })
            } else if (pdfBook != null) {
                PdfScreen(pdfBook!!, pdfBitmap, pdfPage, pdfCount, { pdfPage = (pdfPage - 1).coerceAtLeast(0); pdfBitmap = renderPdf(resolver, Uri.parse(pdfBook!!.pdfUri!!), pdfPage) }, { pdfPage = (pdfPage + 1).coerceAtMost((pdfCount - 1).coerceAtLeast(0)); pdfBitmap = renderPdf(resolver, Uri.parse(pdfBook!!.pdfUri!!), pdfPage) }, { pdfBook = null; pdfBitmap = null })
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("manu novel", fontSize = 29.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = fg); Text("A little world between pages", color = fg.copy(alpha = .65f), fontSize = 13.sp) }
                        IconButton(onClick = { theme = when(theme) { "Light" -> "Sepia"; "Sepia" -> "Dark"; else -> "Light" }; prefs.edit().putString("theme", theme).apply() }) { Icon(Icons.Default.Tune, "Change theme", tint = fg) }
                        IconButton(onClick = { message = "Reading settings are available inside a story." }) { Icon(Icons.Default.Settings, "Settings", tint = fg) }
                    }
                    Row(Modifier.fillMaxWidth().background(fg.copy(alpha = .06f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = fg.copy(alpha=.65f)); Spacer(Modifier.width(8.dp)); BasicTextField(search, { search = it }, Modifier.weight(1f), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color=fg, fontSize=15.sp), decorationBox = { inner -> if(search.isEmpty()) Text("Search title or author", color=fg.copy(alpha=.45f)); inner() })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Mystery", "Fantasy", "PDF").forEach { g -> FilterChip(selected = genre == g, onClick = { genre = g }, label = { Text(g) }) }; Spacer(Modifier.weight(1f)); TextButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Icon(Icons.Default.ImportContacts, null); Text(" Import PDF") } }
                    val filtered = allBooks.filter { (genre == "All" || it.genre == genre) && (it.title.contains(search, true) || it.author.contains(search, true)) }
                    LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("CONTINUE READING", color = fg.copy(alpha=.6f), fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp); Spacer(Modifier.height(8.dp)) }
                        val continueBook = filtered.firstOrNull { (progress[it.id] ?: 0) > 0 } ?: filtered.firstOrNull()
                        if(continueBook != null) item { FeaturedBook(continueBook, progress[continueBook.id] ?: 0, fg, bg) { selected = continueBook; if(continueBook.pdfUri != null) { pdfBook = continueBook; pdfPage = 0; pdfBitmap = renderPdf(resolver, Uri.parse(continueBook.pdfUri), 0); pdfCount = countPdf(resolver, Uri.parse(continueBook.pdfUri)) } else reader = true } }
                        item { Text("RECENTLY ADDED", color = fg.copy(alpha=.6f), fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp); Spacer(Modifier.height(8.dp)) }
                        items(filtered.take(2), key = { it.id }) { b -> BookRow(b, progress[b.id] ?: 0, favorites.contains(b.id), fg, bg, { favorites = if(favorites.contains(b.id)) favorites - b.id else favorites + b.id; prefs.edit().putStringSet("favorites", favorites).apply() }) { selected=b; if(b.pdfUri != null) { pdfBook=b; pdfPage=0; pdfBitmap=renderPdf(resolver, Uri.parse(b.pdfUri),0); pdfCount=countPdf(resolver,Uri.parse(b.pdfUri)) } else reader=true } }
                        item { Text("MY LIBRARY · ${filtered.size}", color = fg.copy(alpha=.6f), fontSize=11.sp, fontWeight=FontWeight.Bold, letterSpacing=1.5.sp); Spacer(Modifier.height(8.dp)) }
                        if(filtered.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment=Alignment.Center) { Text("No books found. Try another search or import a PDF.", color=fg.copy(alpha=.7f)) } }
                        items(filtered, key = { it.id }) { b -> BookRow(b, progress[b.id] ?: 0, favorites.contains(b.id), fg, bg, { favorites = if(favorites.contains(b.id)) favorites - b.id else favorites + b.id; prefs.edit().putStringSet("favorites", favorites).apply() }) { selected=b; if(b.pdfUri != null) { pdfBook=b; pdfPage=0; pdfBitmap=renderPdf(resolver, Uri.parse(b.pdfUri),0); pdfCount=countPdf(resolver,Uri.parse(b.pdfUri)) } else reader=true } }
                    }
                }
                if(message.isNotEmpty()) AlertDialog(onDismissRequest={message=""}, confirmButton={ TextButton(onClick={message=""}) { Text("OK") } }, title={Text("manu novel")}, text={Text(message)})
            }
        }
    }
}

@Composable private fun FeaturedBook(b: Book, progress: Int, fg: Color, bg: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if(fg == Ink) Color(0xFFE8DDC9) else fg.copy(alpha=.08f), RoundedCornerShape(18.dp)).clickable(onClick=onClick).padding(16.dp), verticalAlignment=Alignment.CenterVertically) {
        Cover(b, Modifier.width(94.dp).height(128.dp)); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text("PICK UP WHERE YOU LEFT OFF", fontSize=10.sp, color=fg.copy(alpha=.65f), letterSpacing=1.sp); Spacer(Modifier.height(8.dp)); Text(b.title, fontSize=22.sp, fontFamily=FontFamily.Serif, fontWeight=FontWeight.Bold, color=Ink, maxLines=3, overflow=TextOverflow.Ellipsis); Text(b.author, color=Ink.copy(alpha=.65f), fontSize=13.sp); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress={progress/100f}, modifier=Modifier.fillMaxWidth(), color=Sage, trackColor=Ink.copy(alpha=.12f)); Text("$progress% read", fontSize=11.sp, color=Ink.copy(alpha=.7f)) }
    }
}
@Composable private fun Cover(b: Book, modifier: Modifier = Modifier) { Box(modifier.background(when(b.genre){"Mystery"->Color(0xFF596C60);"Fantasy"->Color(0xFFB87955);else->Color(0xFF8B8068)}, RoundedCornerShape(10.dp)).padding(10.dp), contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("✦", color=Color(0xFFF5E7C8), fontSize=23.sp); Spacer(Modifier.height(8.dp)); Text(b.title, color=Color(0xFFFFF5E3), fontFamily=FontFamily.Serif, fontWeight=FontWeight.Bold, fontSize=12.sp, lineHeight=14.sp, maxLines=4, overflow=TextOverflow.Ellipsis); Spacer(Modifier.height(8.dp)); Text("MANU NOVEL", color=Color(0xFFFFF5E3).copy(alpha=.75f), fontSize=7.sp, letterSpacing=1.sp) } } }
@Composable private fun BookRow(b: Book, progress: Int, favorite: Boolean, fg: Color, bg: Color, onFavorite: () -> Unit, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(fg.copy(alpha=.035f), RoundedCornerShape(14.dp)).clickable(onClick=onClick).padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
        Cover(b, Modifier.width(68.dp).height(92.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(b.title, fontSize=17.sp, fontFamily=FontFamily.Serif, fontWeight=FontWeight.SemiBold, color=fg, maxLines=2, overflow=TextOverflow.Ellipsis); Text(b.author, color=fg.copy(alpha=.65f), fontSize=12.sp); Spacer(Modifier.height(8.dp)); Text(if(b.pdfUri!=null) "PDF · Imported locally" else "${b.genre} · ${progress}% read", color=fg.copy(alpha=.65f), fontSize=11.sp); LinearProgressIndicator(progress={progress/100f}, modifier=Modifier.fillMaxWidth(), color=Sage, trackColor=fg.copy(alpha=.12f)) }; IconButton(onClick=onFavorite) { Icon(if(favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Toggle favourite", tint=if(favorite) Color(0xFFB55E50) else fg.copy(alpha=.55f)) }
    }
}
@Composable private fun ReaderScreen(b: Book, bg: Color, fg: Color, theme: String, onBack: () -> Unit, setTheme: (String) -> Unit, fontSize: Float, setFont: (Float)->Unit, lineScale: Float, setLine: (Float)->Unit, margin: Float, setMargin: (Float)->Unit, progress: Int, onProgress: (Int)->Unit, bookmarked: Boolean, onBookmark: (Boolean)->Unit) {
    var showSettings by remember { mutableStateOf(false) }
    var chapter by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) { IconButton(onClick=onBack) { Icon(Icons.Default.ArrowBack, "Back", tint=fg) }; Column(Modifier.weight(1f)) { Text(b.title, color=fg, fontFamily=FontFamily.Serif, fontWeight=FontWeight.Bold, maxLines=1); Text("Chapter ${chapter+1} of ${b.chapters.size}", color=fg.copy(alpha=.6f), fontSize=11.sp) }; IconButton(onClick={onBookmark(!bookmarked)}) { Icon(if(bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark position", tint=fg) }; IconButton(onClick={showSettings=!showSettings}) { Icon(Icons.Default.Tune, "Reading settings", tint=fg) } }
        if(showSettings) Column(Modifier.fillMaxWidth().padding(horizontal=22.dp).background(fg.copy(alpha=.05f), RoundedCornerShape(14.dp)).padding(14.dp)) { Text("Reading appearance", color=fg, fontWeight=FontWeight.Bold); Text("Font size · ${fontSize.toInt()}", color=fg); Slider(fontSize, setFont, valueRange=14f..34f); Text("Line spacing · ${"%.2f".format(lineScale)}", color=fg); Slider(lineScale, setLine, valueRange=1.1f..2f); Text("Side margins · ${margin.toInt()} dp", color=fg); Slider(margin, setMargin, valueRange=8f..42f) }
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Light","Sepia","Dark").forEach { TextButton(onClick={ setTheme(it) }) { Text(if(it==theme) "• $it" else it, color=fg) } } }
        key(chapter) { LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal=margin.dp), contentPadding=PaddingValues(top=12.dp,bottom=22.dp)) { item { Text(b.chapters.getOrElse(chapter){"A quiet chapter"}, fontFamily=FontFamily.Serif, fontSize=30.sp, lineHeight=36.sp, color=fg, fontWeight=FontWeight.Bold); Spacer(Modifier.height(24.dp)); val paragraphs = b.body.chunked((b.body.size.toFloat()/b.chapters.size).toInt().coerceAtLeast(1)); paragraphs.getOrElse(chapter){b.body}.forEach { p -> Text(p, fontFamily=FontFamily.Serif, fontSize=fontSize.sp, lineHeight=(fontSize*lineScale).sp, color=fg, modifier=Modifier.padding(bottom=18.dp)) } } } }
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=12.dp)) { Slider(progress.toFloat(), { onProgress(it.toInt()) }, valueRange=0f..100f); Row(verticalAlignment=Alignment.CenterVertically) { Text("$progress% read", color=fg.copy(alpha=.65f), fontSize=12.sp); Spacer(Modifier.weight(1f)); TextButton(onClick={chapter=(chapter-1).coerceAtLeast(0);onProgress((chapter*100/b.chapters.size).coerceIn(0,100))}, enabled=chapter>0) { Text("Previous") }; TextButton(onClick={chapter=(chapter+1).coerceAtMost(b.chapters.lastIndex);onProgress(((chapter+1)*100/b.chapters.size).coerceIn(0,100))}, enabled=chapter<b.chapters.lastIndex) { Text("Next chapter") } } }
    }
}
@Composable private fun PdfScreen(book: Book, bitmap: Bitmap?, page: Int, count: Int, previous: ()->Unit, next: ()->Unit, back: ()->Unit) { Column(Modifier.fillMaxSize().padding(12.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { IconButton(onClick=back) { Icon(Icons.Default.ArrowBack,"Back") }; Text(book.title, Modifier.weight(1f), maxLines=1); Text("${page+1}/${count.coerceAtLeast(1)}") }; if(bitmap!=null) Image(bitmap.asImageBitmap(), null, Modifier.weight(1f).fillMaxWidth().padding(6.dp)); else Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment=Alignment.Center) { Text("Unable to display this PDF page. Try another PDF.") }; Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Button(onClick=previous, enabled=page>0) { Text("Previous page") }; Button(onClick=next, enabled=count>0 && page<count-1) { Text("Next page") } } } }

private fun queryName(resolver: ContentResolver, uri: Uri): String? = try { resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if(it.moveToFirst()) it.getString(0) else null } } catch(_: Exception) { null }
private fun renderPdf(resolver: ContentResolver, uri: Uri, page: Int): Bitmap? = try { resolver.openFileDescriptor(uri,"r")?.use { pfd -> PdfRenderer(pfd).use { renderer -> if(renderer.pageCount==0) null else renderer.openPage(page.coerceIn(0,renderer.pageCount-1)).use { p -> val w=1000; val h=(w.toFloat()*p.height/p.width).toInt(); Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also { p.render(it,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } } } } } catch(_: Exception) { null }
private fun countPdf(resolver: ContentResolver, uri: Uri): Int = try { resolver.openFileDescriptor(uri,"r")?.use { PdfRenderer(it).use { r -> r.pageCount } } ?: 0 } catch(_: Exception) { 0 }
private fun loadImported(prefs: android.content.SharedPreferences): List<Book> = try { val arr=JSONArray(prefs.getString("imported","[]")); (0 until arr.length()).map { val o=arr.getJSONObject(it); Book(o.getString("id"),o.getString("title"),o.getString("author"),"PDF", emptyList(),emptyList(),o.getString("uri"),o.optString("name")) } } catch(_: Exception) { emptyList() }
private fun saveImported(prefs: android.content.SharedPreferences, books: List<Book>) { val arr=JSONArray(); books.forEach { b -> if(b.pdfUri!=null) arr.put(JSONObject().put("id",b.id).put("title",b.title).put("author",b.author).put("uri",b.pdfUri).put("name",b.pdfName)) }; prefs.edit().putString("imported",arr.toString()).apply() }
private fun loadProgress(prefs: android.content.SharedPreferences): Map<String,Int> = try { val o=JSONObject(prefs.getString("progress","{}")); o.keys().asSequence().associateWith { o.getInt(it) } } catch(_: Exception) { emptyMap() }
private fun saveProgress(prefs: android.content.SharedPreferences, p: Map<String,Int>) { val o=JSONObject(); p.forEach { (k,v)->o.put(k,v) }; prefs.edit().putString("progress",o.toString()).apply() }
