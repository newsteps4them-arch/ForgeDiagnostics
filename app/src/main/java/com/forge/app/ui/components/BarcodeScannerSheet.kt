package com.forge.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.forge.app.data.InventoryEntity
import com.forge.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerSheet(
    inventory: List<InventoryEntity>,
    onUpdateStock: (InventoryEntity, Int) -> Unit,
    onAddPart: (String, String, String, Int, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    var scannedCode by remember { mutableStateOf("0261500059") }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }
    var customQtyInput by remember { mutableStateOf("") }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }

    // New item registration form state
    var newPartName by remember { mutableStateOf("") }
    var newPartCategory by remember { mutableStateOf("Fuel System") }
    var newPartStock by remember { mutableStateOf("5") }
    var newPartPrice by remember { mutableStateOf("89.99") }
    var newPartBin by remember { mutableStateOf("Bin C-2") }

    // Animated scanner laser line position
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    val matchedItem = remember(scannedCode, inventory) {
        inventory.firstOrNull {
            it.partNumber.equals(scannedCode.trim(), ignoreCase = true) ||
                    it.name.contains(scannedCode.trim(), ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ForgeSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = ForgeAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "WORKSHOP BARCODE & QR SCANNER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeAmber,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Scan parts to adjust stock count in Room DB",
                            fontSize = 11.sp,
                            color = ForgeOnSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = ForgeOnSurface)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasCameraPermission) {
                // Camera Permission Request UI
                Surface(
                    color = ForgeBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = ForgeRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "CAMERA PERMISSION REQUIRED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ForgeRed
                        )
                        Text(
                            text = "Team Forge uses the camera permission to scan physical part barcodes, QR tags, and OEM packaging labels in your workshop.",
                            fontSize = 12.sp,
                            color = ForgeOnSurface,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Button(
                            onClick = { launcher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GRANT CAMERA PERMISSION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Live Camera Viewfinder Overlay Component
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.dp, ForgeBorder, RoundedCornerShape(12.dp))
                ) {
                    // Simulated Live Camera Feed / Scanning Grid
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 3.dp.toPx()
                        val cornerSize = 24.dp.toPx()
                        val boxWidth = size.width * 0.7f
                        val boxHeight = size.height * 0.65f
                        val left = (size.width - boxWidth) / 2
                        val top = (size.height - boxHeight) / 2
                        val right = left + boxWidth
                        val bottom = top + boxHeight

                        // Dark dimming overlay around camera target
                        drawRect(
                            color = Color.Black.copy(alpha = 0.5f)
                        )

                        // Clear target cutout
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(boxWidth, boxHeight)
                        )

                        // Target Corner Brackets
                        val color = if (isFlashOn) ForgeAmber else ForgeCyan

                        // Top Left Corner
                        drawLine(color, Offset(left, top), Offset(left + cornerSize, top), strokeWidth)
                        drawLine(color, Offset(left, top), Offset(left, top + cornerSize), strokeWidth)

                        // Top Right Corner
                        drawLine(color, Offset(right, top), Offset(right - cornerSize, top), strokeWidth)
                        drawLine(color, Offset(right, top), Offset(right, top + cornerSize), strokeWidth)

                        // Bottom Left Corner
                        drawLine(color, Offset(left, bottom), Offset(left + cornerSize, bottom), strokeWidth)
                        drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerSize), strokeWidth)

                        // Bottom Right Corner
                        drawLine(color, Offset(right, bottom), Offset(right - cornerSize, bottom), strokeWidth)
                        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerSize), strokeWidth)

                        // Animated Laser Scan Line
                        val laserY = top + (boxHeight * laserPosition)
                        drawLine(
                            color = ForgeRed,
                            start = Offset(left + 8.dp.toPx(), laserY),
                            end = Offset(right - 8.dp.toPx(), laserY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Top Viewfinder Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ForgeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CAMERA ACTIVE • 60 FPS",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = { isFlashOn = !isFlashOn },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Torch",
                                tint = if (isFlashOn) ForgeAmber else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Bottom Viewfinder Banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Align Barcode / QR Code within target frame",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Barcodes & Manual Barcode Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCAN SIMULATION & MANUAL INPUT",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ForgeAmber
                    )
                    Text(
                        text = "Tap to trigger scan",
                        fontSize = 10.sp,
                        color = ForgeOnSurfaceVariant
                    )
                }

                val sampleBarcodes = listOf(
                    "0261500059" to "Bosch Injector",
                    "06H905601A" to "NGK Spark Plug",
                    "8W0698151" to "Brake Pad Set",
                    "052167M2" to "Synthetic Oil",
                    "999888111" to "Unregistered Part"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleBarcodes) { (code, label) ->
                        val isSelected = scannedCode == code
                        Surface(
                            color = if (isSelected) ForgeAmber.copy(alpha = 0.2f) else ForgeSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ForgeAmber else ForgeBorder
                            ),
                            modifier = Modifier.clickable {
                                scannedCode = code
                                updateSuccessMessage = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = if (isSelected) ForgeAmber else ForgeOnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = code,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ForgeAmber else ForgeOnSurface
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = { manualBarcodeInput = it },
                        placeholder = { Text("Type custom barcode / part #", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForgeCyan,
                            unfocusedBorderColor = ForgeBorder,
                            focusedContainerColor = ForgeBackground,
                            unfocusedContainerColor = ForgeBackground
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualBarcodeInput.isNotBlank()) {
                                scannedCode = manualBarcodeInput.trim()
                                manualBarcodeInput = ""
                                updateSuccessMessage = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SEARCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = ForgeBorder, modifier = Modifier.padding(vertical = 10.dp))

            // Success feedback banner
            if (updateSuccessMessage != null) {
                Surface(
                    color = ForgeGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ForgeGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateSuccessMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForgeGreen
                        )
                    }
                }
            }

            // Scanned Item Match Result
            if (matchedItem != null) {
                // MATCHED INVENTORY ITEM CARD
                Surface(
                    color = ForgeBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ForgeCyan.copy(alpha = 0.2f))
                                        .padding(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = ForgeCyan, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = matchedItem.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForgeOnSurface
                                    )
                                    Text(
                                        text = "PN: ${matchedItem.partNumber} • Location: ${matchedItem.location}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ForgeOnSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (matchedItem.stockQuantity > 0) ForgeGreen.copy(alpha = 0.2f) else ForgeRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${matchedItem.stockQuantity} IN STOCK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (matchedItem.stockQuantity > 0) ForgeGreen else ForgeRed
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Category: ${matchedItem.category}", fontSize = 11.sp, color = ForgeOnSurfaceVariant)
                            Text(text = "Unit Price: \$${matchedItem.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForgeAmber)
                        }

                        HorizontalDivider(color = ForgeBorder)

                        // Room Database Stock Count Adjustment Controls
                        Text(
                            text = "ROOM DB STOCK ADJUSTMENT",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ForgeAmber
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val newQty = (matchedItem.stockQuantity - 1).coerceAtLeast(0)
                                    onUpdateStock(matchedItem, newQty)
                                    updateSuccessMessage = "Decremented stock for ${matchedItem.name} to $newQty units in Room DB"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeRed.copy(alpha = 0.8f), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DEDUCT (-1)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val newQty = matchedItem.stockQuantity + 1
                                    onUpdateStock(matchedItem, newQty)
                                    updateSuccessMessage = "Incremented stock for ${matchedItem.name} to $newQty units in Room DB"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RECEIVE (+1)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Custom Quantity Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customQtyInput,
                                onValueChange = { customQtyInput = it },
                                placeholder = { Text("Set explicit count (e.g., 12)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForgeCyan,
                                    unfocusedBorderColor = ForgeBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val count = customQtyInput.toIntOrNull()
                                    if (count != null && count >= 0) {
                                        onUpdateStock(matchedItem, count)
                                        updateSuccessMessage = "Set stock count for ${matchedItem.name} to $count units in Room DB!"
                                        customQtyInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForgeCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("UPDATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // UNREGISTERED BARCODE - NEW PART FORM
                Surface(
                    color = ForgeBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ForgeAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ForgeAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UNREGISTERED BARCODE: '$scannedCode'",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ForgeAmber
                            )
                        }
                        Text(
                            text = "This item code was not found in Room DB. Register it as a new workshop inventory item below:",
                            fontSize = 11.sp,
                            color = ForgeOnSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newPartName,
                            onValueChange = { newPartName = it },
                            label = { Text("Part Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPartCategory,
                                onValueChange = { newPartCategory = it },
                                label = { Text("Category") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newPartBin,
                                onValueChange = { newPartBin = it },
                                label = { Text("Bin Location") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPartStock,
                                onValueChange = { newPartStock = it },
                                label = { Text("Stock Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newPartPrice,
                                onValueChange = { newPartPrice = it },
                                label = { Text("Price (\$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                val name = if (newPartName.isNotBlank()) newPartName else "Scanned Item ($scannedCode)"
                                val stock = newPartStock.toIntOrNull() ?: 1
                                val price = newPartPrice.toDoubleOrNull() ?: 49.99
                                onAddPart(scannedCode, name, newPartCategory, stock, price, newPartBin)
                                updateSuccessMessage = "Registered new item '$name' in Room Database!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForgeAmber, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE NEW ITEM TO ROOM DATABASE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
