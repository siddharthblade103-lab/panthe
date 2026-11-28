package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.ScholarProfileDetails
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalTryAgainPalette
import com.example.ui.theme.NunitoDisplayFamily

@Composable
fun ScholarProfileDetailsCard(
    details: ScholarProfileDetails,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTryAgainPalette.current
    val tactile = LocalTactileSound.current

    LiquidGlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scholar_profile_details_card"),
        cornerRadius = 26.dp,
        elevated = false,
        accentTint = palette.prismCyan,
        animatedCaustics = false,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Badge,
                        contentDescription = null,
                        tint = palette.primaryLip,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_details_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }
                Surface(
                    modifier = Modifier
                        .testTag("edit_scholar_details_button")
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            tactile?.playTap()
                            onEditDetails()
                        },
                    shape = RoundedCornerShape(50),
                    color = palette.primarySoft,
                    border = BorderStroke(1.dp, palette.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit scholar details",
                            tint = palette.primaryLip,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.profile_details_edit_cta),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = palette.primaryLip
                        )
                    }
                }
            }

            if (details.bio.isNotBlank()) {
                Text(
                    text = details.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
            } else {
                Text(
                    text = "Add your bio, school or course, target exam, study shift, phone, and email to complete your full scholar profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (details.schoolOrCourse.isNotBlank()) {
                    DetailInfoLine(Icons.Rounded.School, "School / Course", details.schoolOrCourse)
                }
                if (details.targetExamOrGoal.isNotBlank()) {
                    DetailInfoLine(Icons.Rounded.Flag, "Target Goal", details.targetExamOrGoal)
                }
                DetailInfoLine(
                    Icons.Rounded.WbSunny,
                    "Study Routine",
                    "${details.favoriteSubject.ifBlank { "Math" }} • ${details.preferredStudyShift.ifBlank { "Morning" }} shift"
                )
                if (details.phoneNumber.isNotBlank()) {
                    DetailInfoLine(Icons.Rounded.PhoneAndroid, "Mobile", details.phoneNumber)
                }
                if (details.preferredEmail.isNotBlank()) {
                    DetailInfoLine(Icons.Rounded.AlternateEmail, "Contact Email", details.preferredEmail)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoLine(
    icon: ImageVector,
    label: String,
    value: String
) {
    val palette = LocalTryAgainPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.primaryLip,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = palette.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.textPrimary
        )
    }
}

@Composable
fun EditScholarDetailsDialog(
    details: ScholarProfileDetails,
    subjects: List<String>,
    onSave: (ScholarProfileDetails) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var bio by remember(details) { mutableStateOf(details.bio) }
    var school by remember(details) { mutableStateOf(details.schoolOrCourse) }
    var targetGoal by remember(details) { mutableStateOf(details.targetExamOrGoal) }
    var favSubject by remember(details) { mutableStateOf(details.favoriteSubject.ifBlank { "Math" }) }
    var studyShift by remember(details) { mutableStateOf(details.preferredStudyShift.ifBlank { "Morning" }) }
    var phone by remember(details) { mutableStateOf(details.phoneNumber) }
    var email by remember(details) { mutableStateOf(details.preferredEmail) }

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("edit_scholar_details_dialog"),
            cornerRadius = 28.dp,
            elevated = true,
            frostedBlur = true,
            accentTint = palette.primary,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Full Scholar Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NunitoDisplayFamily,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = palette.textPrimary
                )

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.glassBorderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(180) },
                    label = { Text("Bio / About Me") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = school,
                    onValueChange = { school = it.take(80) },
                    label = { Text("School / College / Course") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = targetGoal,
                    onValueChange = { targetGoal = it.take(80) },
                    label = { Text("Target Exam or Study Goal") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                Text(
                    text = "Favorite Subject",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjects.ifEmpty { UserProfile.DEFAULT_SUBJECTS }) { subj ->
                        MorphismChip(
                            label = subj,
                            selected = favSubject == subj,
                            onClick = { favSubject = subj }
                        )
                    }
                }

                Text(
                    text = "Preferred Study Shift",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.textSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Morning", "Afternoon", "Evening", "Late Night")) { shift ->
                        MorphismChip(
                            label = shift,
                            selected = studyShift == shift,
                            onClick = { studyShift = shift }
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(20) },
                    label = { Text("Mobile Number (e.g. +91...)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(80) },
                    label = { Text("Email / Zoho Mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Cancel", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                    MorphismActionButton(
                        text = "SAVE PROFILE",
                        onClick = {
                            onSave(
                                details.copy(
                                    bio = bio.trim(),
                                    schoolOrCourse = school.trim(),
                                    targetExamOrGoal = targetGoal.trim(),
                                    favoriteSubject = favSubject,
                                    preferredStudyShift = studyShift,
                                    phoneNumber = phone.trim(),
                                    preferredEmail = email.trim()
                                )
                            )
                        },
                        containerColor = palette.primary,
                        lipColor = palette.primaryLip,
                        height = 46.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_save_scholar_details"
                    )
                }
            }
        }
    }
}

@Composable
fun EmailZohoSignInDialog(
    isBusy: Boolean,
    error: String?,
    onSubmit: (email: String, password: String, createAccount: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_zoho_signin_dialog"),
            cornerRadius = 26.dp,
            elevated = true,
            frostedBlur = true,
            accentTint = palette.gemColor,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = palette.gemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Email & Zoho Mail Sign-In",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }

                SlidingGlassSegmentedBar(
                    labels = listOf("Sign In", "Create Account"),
                    selectedIndex = if (createAccount) 1 else 0,
                    onSelect = { createAccount = it == 1 },
                    tagPrefix = "email_auth_mode"
                )

                // Quick domain helper chip for Zoho Mail
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MorphismChip(
                        label = "@zohomail.in",
                        selected = email.endsWith("@zohomail.in", ignoreCase = true),
                        onClick = {
                            val prefix = email.substringBefore("@").ifBlank { "scholar" }
                            email = "$prefix@zohomail.in"
                        }
                    )
                    MorphismChip(
                        label = "@zoho.com",
                        selected = email.endsWith("@zoho.com", ignoreCase = true),
                        onClick = {
                            val prefix = email.substringBefore("@").ifBlank { "scholar" }
                            email = "$prefix@zoho.com"
                        }
                    )
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.glassBorderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email or Zoho Mail address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (6+ characters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors
                )

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.coralAccent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                    MorphismActionButton(
                        text = when {
                            isBusy -> "PLEASE WAIT…"
                            createAccount -> "CREATE ACCOUNT"
                            else -> "SIGN IN"
                        },
                        onClick = { onSubmit(email, password, createAccount) },
                        enabled = !isBusy,
                        containerColor = palette.gemColor,
                        lipColor = palette.gemLip,
                        height = 46.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_email_zoho_signin"
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneOtpSignInDialog(
    isBusy: Boolean,
    verificationId: String?,
    error: String?,
    onSendOtp: (phoneNumber: String) -> Unit,
    onVerifyOtp: (smsCode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalTryAgainPalette.current
    var phone by remember { mutableStateOf("+91") }
    var otpCode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        MorphismSurface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_otp_signin_dialog"),
            cornerRadius = 26.dp,
            elevated = true,
            frostedBlur = true,
            accentTint = palette.primary,
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = palette.primaryLip,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mobile Number + OTP Login",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoDisplayFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = palette.textPrimary
                    )
                }

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.glassBorderSubtle,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )

                if (verificationId == null) {
                    Text(
                        text = "Enter your mobile number with country code (e.g. +919876543210) to receive an SMS OTP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                } else {
                    Text(
                        text = "OTP sent to $phone! Enter the 6-digit verification code below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.primaryLip
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("6-Digit OTP Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.coralAccent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.textSecondary, fontWeight = FontWeight.Bold)
                    }
                    MorphismActionButton(
                        text = when {
                            isBusy -> "WAIT…"
                            verificationId == null -> "SEND OTP"
                            else -> "VERIFY OTP"
                        },
                        onClick = {
                            if (verificationId == null) onSendOtp(phone) else onVerifyOtp(otpCode)
                        },
                        enabled = !isBusy,
                        containerColor = palette.primary,
                        lipColor = palette.primaryLip,
                        height = 46.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "confirm_phone_otp_action"
                    )
                }
            }
        }
    }
}
