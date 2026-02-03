package com.tradernet.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tblClientApps")
public class ClientAppEntity extends ApplicationEntity {
}
