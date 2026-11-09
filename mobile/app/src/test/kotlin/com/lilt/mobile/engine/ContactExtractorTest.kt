package com.lilt.mobile.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContactExtractorTest {

    @Test
    fun testDirectImperativeEnglish() {
        val contact = ContactExtractor.extract("Save Rajesh's number 98490 12345")
        assertNotNull(contact)
        assertEquals("Rajesh", contact?.name)
        assertEquals("9849012345", contact?.phone)
        assertEquals("98490 12345", contact?.formattedDisplayPhone)
    }

    @Test
    fun testNoteDownForPerson() {
        val contact = ContactExtractor.extract("Note down the number 98765 43210 for Suresh Kumar")
        assertNotNull(contact)
        assertEquals("Suresh Kumar", contact?.name)
        assertEquals("9876543210", contact?.phone)
        assertEquals("98765 43210", contact?.formattedDisplayPhone)
    }

    @Test
    fun testTenglishNumberChesko() {
        val contact = ContactExtractor.extract("Rajesh number note chesko 9849012345")
        assertNotNull(contact)
        assertEquals("Rajesh", contact?.name)
        assertEquals("9849012345", contact?.phone)
    }

    @Test
    fun testTenglishSaveChey() {
        val contact = ContactExtractor.extract("Suresh phone number save chey 98490 54321")
        assertNotNull(contact)
        assertEquals("Suresh", contact?.name)
        assertEquals("9849054321", contact?.phone)
    }

    @Test
    fun testHinglishKaNumber() {
        val contact = ContactExtractor.extract("Rahul ka number save karo 9123456789")
        assertNotNull(contact)
        assertEquals("Rahul", contact?.name)
        assertEquals("9123456789", contact?.phone)
    }

    @Test
    fun testCountryCodeAndColon() {
        val contact = ContactExtractor.extract("Save contact Priya: +91 98490 12345")
        assertNotNull(contact)
        assertEquals("Priya", contact?.name)
        assertEquals("+919849012345", contact?.phone)
        assertEquals("+91 98490 12345", contact?.formattedDisplayPhone)
    }

    @Test
    fun testSpokenMultiplierDigits() {
        val contact = ContactExtractor.extract("double 9 eight 4 9 zero 12345 for Rajesh")
        assertNotNull(contact)
        assertEquals("Rajesh", contact?.name)
        assertEquals("99849012345", contact?.phone)
    }

    @Test
    fun testNoPhoneNumberReturnsNull() {
        val contact = ContactExtractor.extract("Meeting with Rajesh tomorrow at 3 PM")
        assertNull(contact)
    }
}
