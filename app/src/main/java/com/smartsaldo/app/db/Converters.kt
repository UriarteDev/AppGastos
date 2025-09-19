package com.smartsaldo.app.db

import androidx.room.TypeConverter
import com.smartsaldo.app.db.entities.TipoTransaccion

class Converters {

    @TypeConverter
    fun fromTipoTransaccion(tipo: TipoTransaccion): String {
        return tipo.name
    }

    @TypeConverter
    fun toTipoTransaccion(tipo: String): TipoTransaccion {
        return TipoTransaccion.valueOf(tipo)
    }
}