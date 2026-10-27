package com.example.stockmaneger.ui.customers

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerAdapterTest {

    @Test
    fun getAvatarInitial_emptyName_returnsQuestionMark() {
        assertEquals("?", CustomerAdapter.getAvatarInitial(""))
    }

    @Test
    fun getAvatarInitial_whitespaceOnlyName_returnsQuestionMark() {
        assertEquals("?", CustomerAdapter.getAvatarInitial("   "))
        assertEquals("?", CustomerAdapter.getAvatarInitial("\t\n  \r"))
    }

    @Test
    fun getAvatarInitial_validName_returnsUppercaseInitial() {
        assertEquals("J", CustomerAdapter.getAvatarInitial("John"))
        assertEquals("A", CustomerAdapter.getAvatarInitial("alice"))
        assertEquals("M", CustomerAdapter.getAvatarInitial("Mary Doe"))
    }

    @Test
    fun getAvatarInitial_nameWithLeadingWhitespace_returnsFirstNonWhitespaceUppercase() {
        assertEquals("A", CustomerAdapter.getAvatarInitial("   Alice"))
        assertEquals("B", CustomerAdapter.getAvatarInitial("\t\n Bob"))
    }

    @Test
    fun getAvatarInitial_symbolOrDigitName_returnsCharacter() {
        assertEquals("1", CustomerAdapter.getAvatarInitial("123 Company"))
        assertEquals("?", CustomerAdapter.getAvatarInitial("? Unknown"))
    }
}
