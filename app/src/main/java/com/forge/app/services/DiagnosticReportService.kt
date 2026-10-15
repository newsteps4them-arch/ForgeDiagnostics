// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class DiagnosticReportData(
    val reportId: String = "FORGE-RPT-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}",
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
    val vehicleName: String = "2021 Audi S5 Sportback",
    val vehicleVin: String = "WAUZZZF58MA019284",
    val protocol: String = "ISO 15765-4 (CAN 11-bit / 500 kbps)",
    val connectionType: String = "OBD-II CAN Bus Link",
    val technicianName: String = "Team Forge Master Technician",
    val shopName: String = "TEAM FORGE MOTORSPORTS & ADVANCED DIAGNOSTICS",
    val shopAddress: String = "Precision Automotive Engineering Lab • Suite 100",
    val telemetry: ObdTelemetryData = ObdTelemetryData(),
    val dtcList: List<DtcInfo> = emptyList(),
    val ecuNodesSummary: List<String> = listOf(
        "ECM (0x7E0): FAULT - 2 DTCs Stored",
        "TCM (0x7E1): OK - 0 DTCs",
        "ABS/ESP (0x7E2): OK - 0 DTCs",
        "BCM (0x7E3): OK - 0 DTCs",
        "ADAS Radar (0x7E4): OK - 0 DTCs"
    ),
    val aiGuidanceSummary: String = "Recommended Action: Inspect Cylinder 1 & 3 ignition coil packs, verify high-pressure fuel rail pressure sensor voltage (1.2V - 4.5V range), and clean direct injectors.",
    val technicianNotes: String = "Full pre-repair baseline scan completed. Freeze frame parameters recorded. Ready for customer estimate and physical component test bench validation."
)

object DiagnosticReportService {

    /**
     * Generates a plain-text formatted diagnostic report string.
     */
    fun generateFormattedTextReport(data: DiagnosticReportData): String {
        return buildString {
            appendLine("================================================================================")
            appendLine("                     ${data.shopName}")
            appendLine("                   ${data.shopAddress}")
            appendLine("                      VEHICLE HEALTH & DIAGNOSTIC SCAN REPORT")
            appendLine("================================================================================")
            appendLine("Report ID       : ${data.reportId}")
            appendLine("Scan Date & Time: ${data.timestamp}")
            appendLine("Technician      : ${data.technicianName}")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("1. VEHICLE & BUS INTERFACE")
            appendLine("Vehicle Model   : ${data.vehicleName}")
            appendLine("VIN Identifier  : ${data.vehicleVin}")
            appendLine("OBD-II Protocol : ${data.protocol}")
            appendLine("Hardware Link   : ${data.connectionType} (${if (data.telemetry.isConnected) "ONLINE" else "OFFLINE"})")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("2. DIAGNOSTIC TROUBLE CODES (DTCs)")
            if (data.dtcList.isEmpty()) {
                appendLine(">> NO DIAGNOSTIC TROUBLE CODES FOUND (SYSTEM OK)")
            } else {
                appendLine(String.format("%-10s | %-12s | %s", "DTC CODE", "STATUS", "DESCRIPTION"))
                appendLine("-----------+--------------+-----------------------------------------------------")
                data.dtcList.forEach { dtc ->
                    appendLine(String.format("%-10s | %-12s | %s", dtc.code, dtc.status.uppercase(), dtc.description))
                }
            }
            appendLine("--------------------------------------------------------------------------------")
            appendLine("3. LIVE FREEZE-FRAME & SENSOR TELEMETRY SNAPSHOT")
            appendLine(String.format("  %-28s: %-10s | %-28s: %-10s", "Engine Speed (RPM)", "${data.telemetry.rpm} RPM", "Vehicle Speed", "${data.telemetry.speedKmh} km/h"))
            appendLine(String.format("  %-28s: %-10s | %-28s: %-10s", "Coolant Temperature", "${data.telemetry.coolantTempC} °C", "Intake Air Temp", "${data.telemetry.intakeAirTempC} °C"))
            appendLine(String.format("  %-28s: %-10s | %-28s: %-10s", "Throttle Position", "${data.telemetry.throttlePosPct} %", "Boost / MAP Pressure", "${data.telemetry.boostPressurePsi} PSI"))
            appendLine(String.format("  %-28s: %-10s | %-28s: %-10s", "Battery Alternator", "${data.telemetry.batteryVoltage} V", "Engine Oil Pressure", "${data.telemetry.oilPressurePsi} PSI"))
            appendLine(String.format("  %-28s: %-10s | %-28s: %-10s", "Short Term Fuel Trim (STFT)", "${data.telemetry.fuelTrimShortPct} %", "Long Term Fuel Trim (LTFT)", "${data.telemetry.fuelTrimLongPct} %"))
            appendLine("--------------------------------------------------------------------------------")
            appendLine("4. ECU NETWORK TOPOLOGY STATUS")
            data.ecuNodesSummary.forEach { node ->
                appendLine("  • $node")
            }
            appendLine("--------------------------------------------------------------------------------")
            appendLine("5. AI DIAGNOSTIC INSIGHTS & OEM PROCEDURES")
            appendLine(data.aiGuidanceSummary)
            appendLine("--------------------------------------------------------------------------------")
            appendLine("6. TECHNICIAN & WORKSHOP NOTES")
            appendLine(data.technicianNotes)
            appendLine("================================================================================")
            appendLine("Report Generated by Team Forge Automotive Suite • https://forge.automotive.ai")
            appendLine("================================================================================")
        }
    }

    /**
     * Generates a high-quality, printable PDF document using Android Native PdfDocument.
     */
    fun generatePdfReport(context: Context, data: DiagnosticReportData): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 (72 DPI points)
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val darkBgPaint = Paint().apply { color = Color.rgb(18, 20, 29); style = Paint.Style.FILL }
        val accentAmberPaint = Paint().apply { color = Color.rgb(245, 158, 11); style = Paint.Style.FILL }
        val accentCyanPaint = Paint().apply { color = Color.rgb(6, 182, 212); style = Paint.Style.FILL }
        val whiteTextPaint = Paint().apply { color = Color.WHITE; textSize = 14f; isAntiAlias = true; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val titlePaint = Paint().apply { color = Color.WHITE; textSize = 18f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        val subTitlePaint = Paint().apply { color = Color.rgb(200, 205, 220); textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) }
        val sectionHeaderPaint = Paint().apply { color = Color.rgb(245, 158, 11); textSize = 11f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }
        val bodyBoldPaint = Paint().apply { color = Color.rgb(30, 35, 45); textSize = 9.5f; isAntiAlias = true; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
        val bodyRegularPaint = Paint().apply { color = Color.rgb(70, 75, 90); textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL) }
        val bodyMonospacePaint = Paint().apply { color = Color.rgb(30, 35, 45); textSize = 9f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) }
        val dtcCodePaint = Paint().apply { color = Color.rgb(220, 38, 38); textSize = 10f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) }

        val cardBgPaint = Paint().apply { color = Color.rgb(245, 247, 250); style = Paint.Style.FILL }
        val cardBorderPaint = Paint().apply { color = Color.rgb(215, 220, 230); style = Paint.Style.STROKE; strokeWidth = 1f }
        val alertCardBgPaint = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
        val alertCardBorderPaint = Paint().apply { color = Color.rgb(252, 165, 165); style = Paint.Style.STROKE; strokeWidth = 1f }

        // 1. Draw Top Header Bar (Dark Motorsport High-Tech Bar)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 75f, darkBgPaint)
        canvas.drawRect(0f, 72f, pageWidth.toFloat(), 75f, accentAmberPaint)

        canvas.drawText("TEAM FORGE DIAGNOSTICS", 25f, 32f, titlePaint)
        canvas.drawText("VEHICLE HEALTH & OBD-II TELEMETRY SCAN REPORT", 25f, 48f, subTitlePaint)
        canvas.drawText("REPORT #${data.reportId} • ${data.timestamp}", 25f, 62f, subTitlePaint)

        // Top Right Stamp
        val stampPaint = Paint().apply { color = Color.rgb(6, 182, 212); textSize = 10f; isAntiAlias = true; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); textAlign = Paint.Align.RIGHT }
        canvas.drawText("OEM CERTIFIED SCAN", (pageWidth - 25).toFloat(), 35f, stampPaint)
        val liveStatusPaint = Paint().apply { color = if (data.telemetry.isConnected) Color.rgb(34, 197, 94) else Color.rgb(239, 68, 68); textSize = 9f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText(if (data.telemetry.isConnected) "● CAN LINK ACTIVE" else "○ OFFLINE LOG", (pageWidth - 25).toFloat(), 50f, liveStatusPaint)

        var y = 92f

        // Helper function for Section Titles
        fun drawSectionTitle(title: String) {
            canvas.drawRect(25f, y, 28f, y + 12f, accentAmberPaint)
            canvas.drawText(title, 34f, y + 10f, sectionHeaderPaint)
            y += 18f
        }

        // 2. Vehicle Identification Box
        drawSectionTitle("1. VEHICLE & HARDWARE INTERFACE")
        val vehicleBoxRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 48f)
        canvas.drawRoundRect(vehicleBoxRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(vehicleBoxRect, 6f, 6f, cardBorderPaint)

        canvas.drawText("Vehicle Name:", 35f, y + 18f, bodyRegularPaint)
        canvas.drawText(data.vehicleName, 110f, y + 18f, bodyBoldPaint)

        canvas.drawText("VIN Number:", 35f, y + 36f, bodyRegularPaint)
        canvas.drawText(data.vehicleVin, 110f, y + 36f, bodyMonospacePaint)

        canvas.drawText("OBD Protocol:", 300f, y + 18f, bodyRegularPaint)
        canvas.drawText(data.protocol, 380f, y + 18f, bodyBoldPaint)

        canvas.drawText("Technician:", 300f, y + 36f, bodyRegularPaint)
        canvas.drawText(data.technicianName, 380f, y + 36f, bodyRegularPaint)

        y += 60f

        // 3. Diagnostic Trouble Codes (DTCs) Table
        drawSectionTitle("2. DIAGNOSTIC TROUBLE CODES (${data.dtcList.size} FAULTS)")
        if (data.dtcList.isEmpty()) {
            val passBox = RectF(25f, y, (pageWidth - 25).toFloat(), y + 32f)
            val passBgPaint = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
            val passBorderPaint = Paint().apply { color = Color.rgb(187, 247, 208); style = Paint.Style.STROKE; strokeWidth = 1f }
            val passTextPaint = Paint().apply { color = Color.rgb(22, 101, 52); textSize = 10f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            canvas.drawRoundRect(passBox, 6f, 6f, passBgPaint)
            canvas.drawRoundRect(passBox, 6f, 6f, passBorderPaint)
            canvas.drawText("✓ ALL SYSTEMS NORMAL — No Diagnostic Trouble Codes stored in ECU memory.", 38f, y + 20f, passTextPaint)
            y += 42f
        } else {
            // Table Header
            val tableHeadRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 18f)
            val tableHeadPaint = Paint().apply { color = Color.rgb(230, 235, 245); style = Paint.Style.FILL }
            canvas.drawRect(tableHeadRect, tableHeadPaint)
            canvas.drawText("DTC CODE", 35f, y + 13f, bodyBoldPaint)
            canvas.drawText("STATUS", 110f, y + 13f, bodyBoldPaint)
            canvas.drawText("FAULT DESCRIPTION", 185f, y + 13f, bodyBoldPaint)
            y += 22f

            data.dtcList.forEach { dtc ->
                val dtcRowRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 24f)
                canvas.drawRoundRect(dtcRowRect, 4f, 4f, alertCardBgPaint)
                canvas.drawRoundRect(dtcRowRect, 4f, 4f, alertCardBorderPaint)

                canvas.drawText(dtc.code, 35f, y + 16f, dtcCodePaint)
                canvas.drawText(dtc.status.uppercase(), 110f, y + 16f, bodyBoldPaint)

                // Truncate long descriptions if needed
                val desc = if (dtc.description.length > 55) dtc.description.substring(0, 52) + "..." else dtc.description
                canvas.drawText(desc, 185f, y + 16f, bodyRegularPaint)
                y += 28f
            }
            y += 6f
        }

        // 4. Live Sensor Telemetry Snapshot Grid
        drawSectionTitle("3. LIVE OBD-II TELEMETRY & FREEZE-FRAME METRICS")
        val gridHeight = 100f
        val telemetryRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + gridHeight)
        canvas.drawRoundRect(telemetryRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(telemetryRect, 6f, 6f, cardBorderPaint)

        // Column 1
        canvas.drawText("Engine Speed:", 35f, y + 20f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.rpm} RPM", 145f, y + 20f, bodyBoldPaint)

        canvas.drawText("Coolant Temperature:", 35f, y + 40f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.coolantTempC} °C / ${(data.telemetry.coolantTempC * 9 / 5) + 32} °F", 145f, y + 40f, bodyBoldPaint)

        canvas.drawText("Throttle Position:", 35f, y + 60f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.throttlePosPct} %", 145f, y + 60f, bodyBoldPaint)

        canvas.drawText("Short Term Trim (STFT):", 35f, y + 80f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.fuelTrimShortPct} %", 145f, y + 80f, bodyBoldPaint)

        // Column 2
        canvas.drawText("Vehicle Speed:", 310f, y + 20f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.speedKmh} km/h (${(data.telemetry.speedKmh * 0.621371).toInt()} mph)", 420f, y + 20f, bodyBoldPaint)

        canvas.drawText("Intake Air Temp:", 310f, y + 40f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.intakeAirTempC} °C", 420f, y + 40f, bodyBoldPaint)

        canvas.drawText("Manifold Boost (MAP):", 310f, y + 60f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.boostPressurePsi} PSI", 420f, y + 60f, bodyBoldPaint)

        canvas.drawText("Battery Voltage:", 310f, y + 80f, bodyRegularPaint)
        canvas.drawText("${data.telemetry.batteryVoltage} VDC", 420f, y + 80f, bodyBoldPaint)

        y += gridHeight + 14f

        // 5. Network & ECU Module Topology Summary
        drawSectionTitle("4. ECU NETWORK TOPOLOGY HEALTH")
        val busBoxRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 48f)
        canvas.drawRoundRect(busBoxRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(busBoxRect, 6f, 6f, cardBorderPaint)

        data.ecuNodesSummary.take(4).forEachIndexed { i, nodeStr ->
            val col = i % 2
            val row = i / 2
            val xOffset = if (col == 0) 35f else 295f
            val yOffset = y + 20f + (row * 18f)
            canvas.drawText("• $nodeStr", xOffset, yOffset, bodyMonospacePaint)
        }
        y += 58f

        // 6. AI Guidance & Recommended OEM Procedures
        drawSectionTitle("5. AI DIAGNOSTIC GUIDANCE & PROCEDURES")
        val aiBoxRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 46f)
        val aiBoxPaint = Paint().apply { color = Color.rgb(240, 249, 255); style = Paint.Style.FILL }
        val aiBorderPaint = Paint().apply { color = Color.rgb(186, 230, 253); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(aiBoxRect, 6f, 6f, aiBoxPaint)
        canvas.drawRoundRect(aiBoxRect, 6f, 6f, aiBorderPaint)

        val words = data.aiGuidanceSummary.split(" ")
        var line1 = ""
        var line2 = ""
        words.forEach { w ->
            if (line1.length < 80) line1 += "$w " else line2 += "$w "
        }
        canvas.drawText(line1.trim(), 35f, y + 20f, bodyBoldPaint)
        if (line2.isNotBlank()) {
            canvas.drawText(line2.trim(), 35f, y + 36f, bodyRegularPaint)
        }
        y += 56f

        // 7. Technician Signature & Stamp Block
        drawSectionTitle("6. CERTIFICATION & WORKSHOP SIGN-OFF")
        val signBoxRect = RectF(25f, y, (pageWidth - 25).toFloat(), y + 50f)
        canvas.drawRoundRect(signBoxRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(signBoxRect, 6f, 6f, cardBorderPaint)

        canvas.drawText("Master Technician: ${data.technicianName}", 35f, y + 22f, bodyRegularPaint)
        canvas.drawText("Technician Signature: _______________________", 35f, y + 40f, bodyRegularPaint)

        canvas.drawText("Facility: ${data.shopName}", 310f, y + 22f, bodyRegularPaint)
        canvas.drawText("Date Certified: ${data.timestamp}", 310f, y + 40f, bodyRegularPaint)

        // Footer Bar
        val footerY = (pageHeight - 20).toFloat()
        val footerPaint = Paint().apply { color = Color.rgb(150, 155, 170); textSize = 8f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("CONFIDENTIAL & PROPRIETARY — Generated by Team Forge Automotive Suite • https://forge.automotive.ai", (pageWidth / 2).toFloat(), footerY, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache/reports directory
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, "${data.reportId}.pdf")
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Saves text report to file in cache.
     */
    fun generateTextReportFile(context: Context, data: DiagnosticReportData): File {
        val text = generateFormattedTextReport(data)
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val textFile = File(reportsDir, "${data.reportId}.txt")
        textFile.writeText(text)
        return textFile
    }

    /**
     * Shares a report file (PDF or TXT) via Android Intent Chooser.
     */
    fun shareReportFile(context: Context, file: File, mimeType: String = "application/pdf", subject: String = "Vehicle Diagnostic Scan Report") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Attached is the vehicle diagnostic health report generated by Team Forge.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Diagnostic Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share report: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares text content directly to messaging/notes apps.
     */
    fun shareTextReport(context: Context, text: String, subject: String = "Vehicle Diagnostic Scan Report") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, "Share Diagnostic Text Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing text report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies report to Android System Clipboard.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Diagnostic Scan Report", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Prints PDF directly using Android Native PrintManager.
     */
    fun printPdfReport(context: Context, pdfFile: File, jobName: String = "Diagnostic_Report") {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service is unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var input: InputStream? = null
                var output: OutputStream? = null

                try {
                    input = FileInputStream(pdfFile)
                    output = FileOutputStream(destination?.fileDescriptor)

                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } > 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        output.write(buf, 0, bytesRead)
                    }

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try {
                        input?.close()
                        output?.close()
                    } catch (_: Exception) {}
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    /**
     * Saves PDF or Text report permanently to public Documents/Downloads or app external directory.
     */
    fun saveReportLocally(context: Context, sourceFile: File, displayName: String, mimeType: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ForgeReports")
                }

                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    "Saved to Documents/ForgeReports/$displayName"
                } else {
                    fallbackLocalSave(context, sourceFile, displayName)
                }
            } else {
                fallbackLocalSave(context, sourceFile, displayName)
            }
        } catch (e: Exception) {
            fallbackLocalSave(context, sourceFile, displayName)
        }
    }

    private fun fallbackLocalSave(context: Context, sourceFile: File, displayName: String): String {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val destFile = File(destDir, displayName)
        sourceFile.copyTo(destFile, overwrite = true)
        return "Saved to: ${destFile.absolutePath}"
    }
}
