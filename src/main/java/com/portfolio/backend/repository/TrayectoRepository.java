package com.portfolio.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portfolio.backend.model.Trayecto;

/**
*@author Mariano Puchetta
*22 nov. 2022
*/

@Repository
public interface TrayectoRepository extends JpaRepository <Trayecto,Long> {

}
