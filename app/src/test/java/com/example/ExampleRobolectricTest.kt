package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ZORIA", appName)
  }

  @Test
  fun `verify ZORIA repeat phrase detection in Urdu and English`() {
    // Urdu repeat requests
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("مجھے سمجھ نہیں آیا"))
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("دوبارہ بتاؤ"))
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("پھر سے سمجھاؤ"))
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("ایک بار اور بتاؤ"))

    // Roman Urdu repeat requests
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("dobara batao"))
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("samajh nahi aya"))

    // English repeat requests
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("can you repeat that please"))
    assertTrue(com.example.zoria.model.ConversationContext.isRepeatRequest("explain again in simpler words"))

    // Unrelated queries must not trigger repeat
    assertFalse(com.example.zoria.model.ConversationContext.isRepeatRequest("آج موسم کیسا ہے؟"))
    assertFalse(com.example.zoria.model.ConversationContext.isRepeatRequest("what time is it?"))
  }
}
