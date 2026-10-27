package com.example.stockmaneger.ui.reports

import androidx.activity.result.contract.ActivityResultContracts
import java.io.ByteArrayOutputStream
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stockmaneger.data.dao.AccountDao
import com.example.stockmaneger.data.dao.CustomerDao
import com.example.stockmaneger.data.dao.ExpenseDao
import com.example.stockmaneger.data.dao.ProductDao
import com.example.stockmaneger.data.dao.SaleDao
import com.example.stockmaneger.databinding.FragmentReportsBinding
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private lateinit var saleDao: SaleDao
    private lateinit var customerDao: CustomerDao
    private lateinit var productDao: ProductDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var accountDao: AccountDao

    // ── Change your business name here ──
    private var BUSINESS_NAME = "Stock Manager"

    //////
    private var pendingWorkbook: XSSFWorkbook? = null
    private var pendingPdfBytes: ByteArray? = null

    private val excelSaver =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        ) { uri ->

            if (uri == null) {
                pendingWorkbook?.close()
                pendingWorkbook = null
                return@registerForActivityResult
            }

            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                    pendingWorkbook?.write(stream)
                }

                pendingWorkbook?.close()
                pendingWorkbook = null

                Toast.makeText(
                    requireContext(),
                    "Excel file saved",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val pdfSaver =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->

            if (uri == null) {
                pendingPdfBytes = null
                return@registerForActivityResult
            }

            try {
                requireContext().contentResolver.openOutputStream(uri)?.use {
                    it.write(pendingPdfBytes)
                }

                pendingPdfBytes = null

                Toast.makeText(
                    requireContext(),
                    "PDF file saved",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saleDao     = SaleDao(requireContext())
        customerDao = CustomerDao(requireContext())
        productDao  = ProductDao(requireContext())
        expenseDao  = ExpenseDao(requireContext())
        accountDao  = AccountDao(requireContext())

        BUSINESS_NAME = accountDao.getBusinessName()

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSalesPdf.setOnClickListener     { exportSalesPdf() }
        binding.btnCustomersPdf.setOnClickListener { exportCustomersPdf() }
        binding.btnStockPdf.setOnClickListener     { exportStockPdf() }
        binding.btnSalesExcel.setOnClickListener   { exportSalesExcel() }
        binding.btnStockExcel.setOnClickListener   { exportStockExcel() }
        binding.btnCustomersExcel.setOnClickListener { exportCustomersExcel() }
    }

    // ════════════════════════════════════════
    // PDF EXPORTS
    // ════════════════════════════════════════

    private fun exportSalesPdf() {
        try {
            val sales    = saleDao.getAll()
            val fileName = "Sales_Report_${today()}.pdf"
            val document = Document()
            val output = ByteArrayOutputStream()

            PdfWriter.getInstance(
                document,
                output
            )
            document.open()
            // Header
            addPdfHeader(document, "Sales Report")

            // Summary
            val totalRevenue = saleDao.getTotalRevenue()
            val totalProfit  = saleDao.getTotalProfit()
            document.add(Paragraph("Total Revenue: $${String.format("%.2f", totalRevenue)}", boldFont()))
            document.add(Paragraph("Total Profit:  $${String.format("%.2f", totalProfit)}", boldFont()))
            document.add(Paragraph(" "))

            // Table
            val table = PdfPTable(7)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(1.5f, 2f, 2f, 1f, 1.5f, 1.5f, 2f))

            addPdfHeader(table, listOf("Date", "Customer", "Product", "Qty", "Price", "Profit", "Payment"))

            sales.forEach { sale ->
                addPdfRow(table, listOf(
                    sale.date,
                    sale.customerName,
                    sale.productName,
                    sale.quantity.toString(),
                    "$${String.format("%.2f", sale.totalPrice)}",
                    "$${String.format("%.2f", sale.profit)}",
                    sale.paymentMethod
                ))
            }

            document.add(table)
            document.close()
            pendingPdfBytes = output.toByteArray()
            pdfSaver.launch("Sales_Report_${today()}.pdf")

            Toast.makeText(requireContext(), "Sales PDF exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportCustomersPdf() {
        try {
            val customers = customerDao.getAll()
            val fileName  = "Customers_Report_${today()}.pdf"
            val document  = Document()
            val output = ByteArrayOutputStream()

            PdfWriter.getInstance(
                document,
                output
            )
            document.open()

            addPdfHeader(document, "Customers Report")
            document.add(Paragraph("Total Customers: ${customers.size}", boldFont()))
            document.add(Paragraph(" "))

            val table = PdfPTable(3)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(1f, 3f, 2f))

            addPdfHeader(table, listOf("#", "Name", "Phone"))

            customers.forEachIndexed { i, customer ->
                addPdfRow(table, listOf(
                    (i + 1).toString(),
                    customer.name,
                    customer.phone.ifEmpty { "—" }
                ))
            }

            document.add(table)
            document.close()

            pendingPdfBytes = output.toByteArray()
            pdfSaver.launch("Sales_Report_${today()}.pdf")
            Toast.makeText(requireContext(), "Customers PDF exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportStockPdf() {
        try {
            val products = productDao.getAll()
            val fileName = "Stock_Report_${today()}.pdf"
            val document = Document()
            val output = ByteArrayOutputStream()

            PdfWriter.getInstance(
                document,
                output
            )
            document.open()

            addPdfHeader(document, "Stock Report")
            document.add(Paragraph("Total Products: ${products.size}", boldFont()))
            document.add(Paragraph(" "))

            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(2.5f, 1.5f, 1.5f, 1.5f, 1.5f))

            addPdfHeader(table, listOf("Product", "Qty", "Base Price", "Sale Price", "Status"))

            products.forEach { product ->
                val status = when {
                    product.quantity == 0  -> "Out of Stock"
                    product.quantity <= 3  -> "Low Stock"
                    else                   -> "In Stock"
                }
                addPdfRow(table, listOf(
                    product.name,
                    product.quantity.toString(),
                    "$${String.format("%.2f", product.basePrice)}",
                    "$${String.format("%.2f", product.salePrice)}",
                    status
                ))
            }

            document.add(table)
            document.close()

            pendingPdfBytes = output.toByteArray()
            pdfSaver.launch("Sales_Report_${today()}.pdf")
            Toast.makeText(requireContext(), "Stock PDF exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ════════════════════════════════════════
    // EXCEL EXPORTS
    // ════════════════════════════════════════

    private fun exportSalesExcel() {
        try {
            val sales    = saleDao.getAll()
            val workbook = XSSFWorkbook()
            val sheet    = workbook.createSheet("Sales")

            // Header row
            val headerRow = sheet.createRow(0)
            listOf("Date", "Customer", "Product", "Qty", "Unit Price", "Total", "Profit", "Payment").forEachIndexed { i, title ->
                headerRow.createCell(i).setCellValue(title)
            }

            // Data rows
            sales.forEachIndexed { index, sale ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(sale.date)
                row.createCell(1).setCellValue(sale.customerName)
                row.createCell(2).setCellValue(sale.productName)
                row.createCell(3).setCellValue(sale.quantity.toDouble())
                row.createCell(4).setCellValue(sale.unitPrice)
                row.createCell(5).setCellValue(sale.totalPrice)
                row.createCell(6).setCellValue(sale.profit)
                row.createCell(7).setCellValue(sale.paymentMethod)
            }

            // Auto size columns
            sheet.setColumnWidth(0, 5000) // Date
            sheet.setColumnWidth(1, 7000) // Customer
            sheet.setColumnWidth(2, 7000) // Product
            sheet.setColumnWidth(3, 3000) // Qty
            sheet.setColumnWidth(4, 4000) // Unit Price
            sheet.setColumnWidth(5, 4000) // Total
            sheet.setColumnWidth(6, 4000) // Profit
            sheet.setColumnWidth(7, 5000) // Payment

            pendingWorkbook = workbook
            excelSaver.launch("Sales_${today()}.xlsx")
            Toast.makeText(requireContext(), "Sales Excel exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportStockExcel() {
        try {
            val products = productDao.getAll()
            val workbook = XSSFWorkbook()
            val sheet    = workbook.createSheet("Stock")

            val headerRow = sheet.createRow(0)
            listOf("Product", "Quantity", "Base Price", "Sale Price", "Status").forEachIndexed { i, title ->
                headerRow.createCell(i).setCellValue(title)
            }

            products.forEachIndexed { index, product ->
                val row    = sheet.createRow(index + 1)
                val status = when {
                    product.quantity == 0 -> "Out of Stock"
                    product.quantity <= 3 -> "Low Stock"
                    else                  -> "In Stock"
                }
                row.createCell(0).setCellValue(product.name)
                row.createCell(1).setCellValue(product.quantity.toDouble())
                row.createCell(2).setCellValue(product.basePrice)
                row.createCell(3).setCellValue(product.salePrice)
                row.createCell(4).setCellValue(status)
            }

            sheet.setColumnWidth(0, 8000) // Product
            sheet.setColumnWidth(1, 3000) // Quantity
            sheet.setColumnWidth(2, 4000) // Base Price
            sheet.setColumnWidth(3, 4000) // Sale Price
            sheet.setColumnWidth(4, 5000) // Status

            pendingWorkbook = workbook
            excelSaver.launch("Sales_${today()}.xlsx")
            Toast.makeText(requireContext(), "Stock Excel exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportCustomersExcel() {
        try {
            val customers = customerDao.getAll()
            val workbook  = XSSFWorkbook()
            val sheet     = workbook.createSheet("Customers")

            val headerRow = sheet.createRow(0)
            listOf("Name", "Phone", "Total Sales", "Total Profit").forEachIndexed { i, title ->
                headerRow.createCell(i).setCellValue(title)
            }

            customers.forEachIndexed { index, customer ->
                val row          = sheet.createRow(index + 1)
                val customerSales = saleDao.getByCustomer(customer.id)
                val totalProfit  = customerSales.sumOf { it.profit }

                row.createCell(0).setCellValue(customer.name)
                row.createCell(1).setCellValue(customer.phone.ifEmpty { "—" })
                row.createCell(2).setCellValue(customerSales.size.toDouble())
                row.createCell(3).setCellValue(totalProfit)
            }

            sheet.setColumnWidth(0, 7000) // Name
            sheet.setColumnWidth(1, 5000) // Phone
            sheet.setColumnWidth(2, 4000) // Total Sales
            sheet.setColumnWidth(3, 4000) // Total Profit

            pendingWorkbook = workbook
            excelSaver.launch("Sales_${today()}.xlsx")
            Toast.makeText(requireContext(), "Customers Excel exported", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════

    private fun addPdfHeader(document: Document, title: String) {
        val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
        val subFont   = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.GRAY)

        document.add(Paragraph(BUSINESS_NAME, titleFont))
        document.add(Paragraph(title, subFont))
        document.add(Paragraph("Generated: ${today()}", subFont))
        document.add(Paragraph(" "))
    }

    private fun addPdfHeader(table: PdfPTable, headers: List<String>) {
        val headerFont  = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor.WHITE)
        val headerColor = BaseColor(26, 86, 219) // matches @color/primary

        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, headerFont))
            cell.backgroundColor = headerColor
            cell.setPadding(6f)
            table.addCell(cell)
        }
    }

    private fun addPdfRow(table: PdfPTable, values: List<String>) {
        val cellFont = Font(Font.FontFamily.HELVETICA, 9f)
        values.forEach { value ->
            val cell = PdfPCell(Phrase(value, cellFont))
            cell.setPadding(5f)
            table.addCell(cell)
        }
    }

    private fun boldFont() = Font(Font.FontFamily.HELVETICA, 11f, Font.BOLD)

    private fun getOutputFile(fileName: String): File {
        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: requireContext().filesDir
        return File(dir, fileName)
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Open with"))
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}