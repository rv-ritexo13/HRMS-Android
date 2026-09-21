package com.triotech.hrms.data.model;

import androidx.annotation.NonNull;

/**
 * A single month's salary record for an employee — the model behind the Salary
 * dashboard, the salary breakdown, the history list and the generated payslip PDF.
 *
 * <p>Amounts are whole rupees (no paise) to keep the demo's mock data and the
 * on-screen/PDF formatting clean. Gross, total-deductions and net are computed
 * from the individual components so a payslip can never show internally
 * inconsistent totals.</p>
 *
 * <p>Backed by the {@code payslips} table in
 * {@link com.triotech.hrms.data.local.HrmsDatabase}; see
 * {@link com.triotech.hrms.data.repository.DbSalaryRepository}.</p>
 */
public final class Payslip {

    /** Stable "yyyy-MM" key, used for lookups, sorting and nav arguments. */
    private final String monthKey;
    private final int year;
    /** 1-12. */
    private final int month;

    private final String employeeId;
    private final String employeeName;
    private final String department;
    private final String designation;

    // Earnings
    private final long basic;
    private final long hra;
    private final long specialAllowance;
    private final long otherAllowances;

    // Deductions
    private final long providentFund;
    private final long professionalTax;
    private final long otherDeductions;

    private final long creditDateMillis;
    private final boolean credited;

    public Payslip(
            @NonNull String monthKey,
            int year,
            int month,
            @NonNull String employeeId,
            @NonNull String employeeName,
            @NonNull String department,
            @NonNull String designation,
            long basic,
            long hra,
            long specialAllowance,
            long otherAllowances,
            long providentFund,
            long professionalTax,
            long otherDeductions,
            long creditDateMillis,
            boolean credited) {
        this.monthKey = monthKey;
        this.year = year;
        this.month = month;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.department = department;
        this.designation = designation;
        this.basic = basic;
        this.hra = hra;
        this.specialAllowance = specialAllowance;
        this.otherAllowances = otherAllowances;
        this.providentFund = providentFund;
        this.professionalTax = professionalTax;
        this.otherDeductions = otherDeductions;
        this.creditDateMillis = creditDateMillis;
        this.credited = credited;
    }

    @NonNull
    public String getMonthKey() {
        return monthKey;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    @NonNull
    public String getEmployeeId() {
        return employeeId;
    }

    @NonNull
    public String getEmployeeName() {
        return employeeName;
    }

    @NonNull
    public String getDepartment() {
        return department;
    }

    @NonNull
    public String getDesignation() {
        return designation;
    }

    public long getBasic() {
        return basic;
    }

    public long getHra() {
        return hra;
    }

    public long getSpecialAllowance() {
        return specialAllowance;
    }

    public long getOtherAllowances() {
        return otherAllowances;
    }

    public long getProvidentFund() {
        return providentFund;
    }

    public long getProfessionalTax() {
        return professionalTax;
    }

    public long getOtherDeductions() {
        return otherDeductions;
    }

    public long getCreditDateMillis() {
        return creditDateMillis;
    }

    public boolean isCredited() {
        return credited;
    }

    public long getGrossSalary() {
        return basic + hra + specialAllowance + otherAllowances;
    }

    public long getTotalDeductions() {
        return providentFund + professionalTax + otherDeductions;
    }

    public long getNetSalary() {
        return getGrossSalary() - getTotalDeductions();
    }
}
