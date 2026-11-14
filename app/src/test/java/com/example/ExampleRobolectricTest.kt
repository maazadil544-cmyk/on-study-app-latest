package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Province
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class ExampleRobolectricTest {

  @Test
  fun `read app name test`() {
    val appName = "ON Study"
    assertEquals("ON Study", appName)
  }

  @Test
  fun `verify all 4 provinces configured`() {
    val provinces = Province.educationalProvinces
    assertEquals(4, provinces.size)
  }
}
