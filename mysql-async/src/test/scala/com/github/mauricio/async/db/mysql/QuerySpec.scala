/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.mysql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.mysql.exceptions.MySQLException
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.buffer.ChannelBuffers
import org.joda.time.{ReadableDateTime, LocalTime, LocalDate, ReadablePartial}

class QuerySpec extends Specification with ConnectionHelper {

  final val createTable = """CREATE TEMPORARY TABLE users (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8' NOT NULL ,
                              PRIMARY KEY (id) );"""
  final val insert = """INSERT INTO users (name) VALUES ('Maurício Aragão')"""
  final val select = """SELECT * FROM users"""

  "connection" should {

    "be able to run a DML query" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable).rowsAffected === 0
      }

    }

    "raise an exception upon a bad statement" in {
      withConnection {
        connection =>
          executeQuery(connection, "this is not SQL") must throwA[MySQLException].like {
            case e => e.asInstanceOf[MySQLException].errorMessage.sqlState === "#42000"
          }
      }
    }

    "be able to select from a table" in {

      withConnection {
        connection =>
          executeQuery(connection, this.createTable).rowsAffected === 0
          executeQuery(connection, this.insert).rowsAffected === 1
          val result = executeQuery(connection, this.select).rows.get

          result(0)("id") === 1
          result(0)("name") === "Maurício Aragão"
      }

    }

    val createTableTimeColumns =
      """CREATE TEMPORARY TABLE posts (
       id INT NOT NULL AUTO_INCREMENT,
       created_at_date DATE not null,
       created_at_datetime DATETIME not null,
       created_at_timestamp TIMESTAMP not null,
       created_at_time TIME not null,
       created_at_year YEAR not null,
       primary key (id)
      )"""

    val insertTableTimeColumns =
      """
        |insert into posts (created_at_date, created_at_datetime, created_at_timestamp, created_at_time, created_at_year)
        |values ( '2038-01-19', '2013-01-19 03:14:07', '2020-01-19 03:14:07', '03:14:07', '1999' )
      """.stripMargin

    "be able to select from a table with timestamps" in {

      withConnection {
        connection =>
          executeQuery(connection, createTableTimeColumns)
          executeQuery(connection, insertTableTimeColumns)
          val result = executeQuery(connection, "SELECT * FROM posts").rows.get(0)

          val date = result("created_at_date").asInstanceOf[LocalDate]

          date.getYear === 2038
          date.getMonthOfYear === 1
          date.getDayOfMonth === 19

          val dateTime = result("created_at_datetime").asInstanceOf[ReadableDateTime]
          dateTime.getYear === 2013
          dateTime.getMonthOfYear === 1
          dateTime.getDayOfMonth === 19
          dateTime.getHourOfDay === 3
          dateTime.getMinuteOfHour === 14
          dateTime.getSecondOfMinute === 7

          val timestamp = result("created_at_timestamp").asInstanceOf[ReadableDateTime]
          timestamp.getYear === 2020
          timestamp.getMonthOfYear === 1
          timestamp.getDayOfMonth === 19
          timestamp.getHourOfDay === 3
          timestamp.getMinuteOfHour === 14
          timestamp.getSecondOfMinute === 7


          val time = result("created_at_time").asInstanceOf[LocalTime]
          time.getHourOfDay === 3
          time.getMinuteOfHour === 14
          time.getSecondOfMinute === 7

          val year = result("created_at_year").asInstanceOf[Int]

          year === 1999


      }

    }

  }

}