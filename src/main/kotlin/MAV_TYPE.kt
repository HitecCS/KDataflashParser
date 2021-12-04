/**
 * MAV_TYPE
 * Copyright (C) 2021 Hitec Commercial Solutions
 * @author Stephen Woerner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software is based on:
 * APM DataFlash log file reader
 * Copyright Andrew Tridgell 2011
 *
 * Released under GNU GPL version 3 or later
 * Partly based on SDLog2Parser by Anton Babushkin
 */
enum class MAV_TYPE {
    // copter
    MAV_TYPE_HELICOPTER,
    MAV_TYPE_TRICOPTER,
    MAV_TYPE_QUADROTOR,
    MAV_TYPE_HEXAROTOR,
    MAV_TYPE_OCTOROTOR,
    MAV_TYPE_DECAROTOR,
    MAV_TYPE_DODECAROTOR,
    MAV_TYPE_COAXIAL,

    // plane
    MAV_TYPE_FIXED_WING,

    // rover
    MAV_TYPE_GROUND_ROVER,
    // boat
    MAV_TYPE_SURFACE_BOAT,
    // tracker
    MAV_TYPE_ANTENNA_TRACKER,
    // sub
    MAV_TYPE_SUBMARINE;
}