/*
 * Copyright 2019-present GT RARE project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed On an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _TABLE_SIZE_P4_
#define _TABLE_SIZE_P4_

#define MIN_TABLE_SIZE                        1024

#define IPV4_LPM_TABLE_SIZE                    MIN_TABLE_SIZE
#define IPV6_LPM_TABLE_SIZE                    MIN_TABLE_SIZE
#define IPV4_HOST_TABLE_SIZE                   MIN_TABLE_SIZE
#define IPV6_HOST_TABLE_SIZE                   MIN_TABLE_SIZE
#define MPLS_TABLE_SIZE                        MIN_TABLE_SIZE
#define VRF_TABLE_SIZE                         MIN_TABLE_SIZE

#endif // _TABLE_SIZE_P4_
