/**
 * Copyright 2023 Ling-Hsiung Yuan and The NetArtisan.Org GCC4AEM Authors
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
let t=0;
const tc= "tc";
let k = t + 1;
console.log(tc + k);

const clsA = {
	n: '',
	f: function(p) {
		console.log(this.n + p);
	}
}

const A = Object.create(clsA);

class clsB {
	n = 'Test';
	constructor(a) {
		this.n = a;
	}
	toString() {
		return `${this.n}`; 
	}
}

const B = new clsB('hello');