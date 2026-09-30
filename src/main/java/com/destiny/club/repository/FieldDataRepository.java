package com.destiny.club.repository;

import com.destiny.club.domain.fielddata.FieldData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FieldDataRepository extends JpaRepository<FieldData, Long> {

    @Query("select f from FieldData f order by f.savingsTransaction.transactionDate desc, f.id desc")
    List<FieldData> findAllOrderByDateDesc();

    @Query("select f from FieldData f where f.savingsTransaction.transactionDate between :fromDate and :toDate "
            + "order by f.savingsTransaction.transactionDate desc, f.id desc")
    List<FieldData> findByTransactionDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select distinct f.collectorOneName from FieldData f "
            + "union select distinct f.collectorTwoName from FieldData f where f.collectorTwoName is not null")
    List<String> findDistinctCollectorNames();
}
