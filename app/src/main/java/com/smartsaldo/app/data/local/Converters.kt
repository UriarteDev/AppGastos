package com.smartsaldo.app.data.local

import androidx.room.TypeConverter
import com.smartsaldo.app.data.local.entities.TipoTransaccion

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