package com.example.phantoms.presentation.activity

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.phantoms.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginActivityUiTest {

    @Test
    fun views_are_visible_on_launch() {
        ActivityScenario.launch(LoginActivity::class.java)

        onView(withId(R.id.emailEt)).check(matches(isDisplayed()))
        onView(withId(R.id.PassEt)).check(matches(isDisplayed()))
        onView(withId(R.id.loginBtn)).check(matches(isDisplayed()))
    }
}
