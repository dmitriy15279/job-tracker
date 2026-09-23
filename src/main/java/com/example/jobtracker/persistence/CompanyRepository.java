package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.Company;
import com.example.jobtracker.persistence.entity.UserType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository
        extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    boolean existsByNameIgnoreCase(String name);

    /**
     * Whether some user of the given type has this company as their only company,
     * i.e. deleting the company would leave that user without one.
     */
    @Query("""
            select case when count(u) > 0 then true else false end
            from User u join u.companies c
            where c.id = :companyId and u.userType = :userType and size(u.companies) = 1
            """)
    boolean existsUserWithOnlyCompany(@Param("companyId") UUID companyId, @Param("userType") UserType userType);
}
