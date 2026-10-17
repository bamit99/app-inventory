package com.example.appinventory

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.appinventory.ui.PermissionGate

@RunWith(AndroidJUnit4::class)
class PermissionGateTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun showsGrantWhenNoAccess() {
        composeTestRule.setContent { PermissionGate(false, {}) { Text("hidden") } }
        composeTestRule.onNodeWithText("Grant Usage Access").assertExists()
        composeTestRule.onNodeWithText("hidden").assertExists()
    }

    @Test fun showsContentWhenGranted() {
        composeTestRule.setContent { PermissionGate(true, {}) { Text("list") } }
        composeTestRule.onNodeWithText("list").assertExists()
        composeTestRule.onNodeWithText("Grant Usage Access").assertDoesNotExist()
    }
}
