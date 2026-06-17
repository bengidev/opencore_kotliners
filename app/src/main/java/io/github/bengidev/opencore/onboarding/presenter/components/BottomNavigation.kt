package io.github.bengidev.opencore.onboarding.presenter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.bengidev.opencore.onboarding.theme.OnboardingTheme

/** Bottom navigation — iOS OnboardingBottomNavigationView (24dp gap, 52dp buttons). */
@Composable
internal fun OnboardingBottomNavigation(
    currentPage: Int,
    totalPages: Int,
    isLastPage: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingProgressIndicator(
            currentPage = currentPage,
            totalPages = totalPages,
            onPageSelected = onPageSelected
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 0) {
                SecondaryButton(
                    text = "BACK",
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = OnboardingTheme.palette.textPrimary
                        )
                    }
                )
            }

            PrimaryButton(
                text = if (isLastPage) "ENTER OPENCORE" else "CONTINUE",
                onClick = if (isLastPage) onFinish else onNext,
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OnboardingTheme.palette.controlStrongText
                    )
                }
            )
        }
    }
}
