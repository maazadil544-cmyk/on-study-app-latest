package com.example

import com.example.data.local.BookEntity
import com.example.ui.viewmodel.StudyViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun provincialGrammarBook_isIdentifiedCorrectly_andExcludedFromGeneralBooks() {
        val kpkGrammarBook = BookEntity(
            title = "KPK Board English Grammar & Composition",
            provinceCode = "kpk",
            classLevel = 0,
            subject = "English Grammar",
            bookType = "GUIDE",
            fileLink = "https://example.com/kpk.pdf",
            publisher = "KPTB"
        )

        // Must be identified as provincial grammar
        assertTrue("KPK grammar book must be recognized as provincial grammar", StudyViewModel.isProvincialGrammarBook(kpkGrammarBook))
        // Must NOT be identified as general book
        assertFalse("KPK grammar book must NEVER be recognized as general book", StudyViewModel.isGeneralBook(kpkGrammarBook))
    }

    @Test
    fun provincialGeneralBook_isIdentifiedCorrectly() {
        val kpkGeneralBook = BookEntity(
            title = "KPK General Knowledge & Informative Guide",
            provinceCode = "kpk",
            classLevel = -1,
            subject = "General Knowledge",
            bookType = "GENERAL",
            fileLink = "https://example.com/kpk_gk.pdf",
            publisher = "KPTB"
        )

        // Must NOT be provincial grammar
        assertFalse("KPK general book is not grammar", StudyViewModel.isProvincialGrammarBook(kpkGeneralBook))
        // Must be recognized as general book
        assertTrue("KPK general book must be recognized as general book", StudyViewModel.isGeneralBook(kpkGeneralBook))
    }

    @Test
    fun nationalGeneralBook_isIdentifiedAsGeneral() {
        val nationalBook = BookEntity(
            title = "Everyday Science & Computer Basics",
            provinceCode = "general",
            classLevel = 0,
            subject = "Computer Skills",
            bookType = "GENERAL",
            fileLink = "https://example.com/general.pdf"
        )

        assertFalse(StudyViewModel.isProvincialGrammarBook(nationalBook))
        assertTrue(StudyViewModel.isGeneralBook(nationalBook))
    }

    @Test
    fun adminCredentials_matchExpectedValues() {
        assertEquals("onstudy13", StudyViewModel.ADMIN_USERNAME)
        assertEquals("onstudy13@gmail.com", StudyViewModel.ADMIN_EMAIL)
        assertEquals("onstudy13.1311", StudyViewModel.ADMIN_PASSWORD)
    }
}

