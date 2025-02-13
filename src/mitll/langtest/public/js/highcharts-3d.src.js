/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies 
 * and their contractors; 2015. Other request for this document shall be referred 
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted 
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA). 
 * Transfer of this data by any means to a non-US person who is not eligible to 
 * obtain export-controlled data is prohibited. By accepting this data, the consignee 
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For 
 * unclassified, limited distribution documents, destroy by any method that will 
 * prevent disclosure of the contents or reconstruction of the document.
 *  
 * This material is based upon work supported under Air Force Contract No. 
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions 
 * or recommendations expressed in this material are those of the author(s) and 
 * do not necessarily reflect the views of the U.S. Air Force.
 *  
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS 
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice, 
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or 
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically 
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

// ==ClosureCompiler==
// @compilation_level SIMPLE_OPTIMIZATIONS

/**
 * @license Highcharts JS v4.1.9 (2015-10-07)
 *
 * (c) 2009-2013 Torstein Hønsi
 *
 * License: www.highcharts.com/license
 */

// JSLint options:
/*global Highcharts, HighchartsAdapter, document, window, navigator, setInterval, clearInterval, clearTimeout, setTimeout, location, jQuery, $, console */

(function (Highcharts) {
	/**
	Shorthands for often used function
*/ 
/**
 *	Mathematical Functionility
 */
var PI = Math.PI,
	deg2rad = (PI / 180), // degrees to radians 
	sin = Math.sin,
	cos = Math.cos, 
	pick = Highcharts.pick,
	round = Math.round;

/**
 * Transforms a given array of points according to the angles in chart.options.
 * Parameters: 
 *		- points: the array of points
 *		- chart: the chart
 *		- insidePlotArea: wether to verifiy the points are inside the plotArea
 * Returns:
 *		- an array of transformed points
 */
function perspective(points, chart, insidePlotArea) {
	var options3d = chart.options.chart.options3d,
		inverted = false,
		origin;

	if (insidePlotArea) {
		inverted = chart.inverted;
		origin = {
			x: chart.plotWidth / 2,
			y: chart.plotHeight / 2,
			z: options3d.depth / 2,
			vd: pick(options3d.depth, 1) * pick(options3d.viewDistance, 0)
		};
	} else {
		origin = {
			x: chart.plotLeft + (chart.plotWidth / 2),
			y: chart.plotTop + (chart.plotHeight / 2),
			z: options3d.depth / 2,
			vd: pick(options3d.depth, 1) * pick(options3d.viewDistance, 0)
		};
	}

	var result = [],
		xe = origin.x,
		ye = origin.y,
		ze = origin.z,
		vd = origin.vd,
		angle1 = deg2rad * (inverted ?  options3d.beta  : -options3d.beta),
		angle2 = deg2rad * (inverted ? -options3d.alpha :  options3d.alpha),
		s1 = sin(angle1),
		c1 = cos(angle1),
		s2 = sin(angle2),
		c2 = cos(angle2);

	var x, y, z, px, py, pz;

	// Transform each point
	Highcharts.each(points, function (point) {
		x = (inverted ? point.y : point.x) - xe;
		y = (inverted ? point.x : point.y) - ye;
		z = (point.z || 0) - ze;

		//Apply 3-D rotation
		px = c1 * x - s1 * z;
		py = -s1 * s2 * x - c1 * s2 * z + c2 * y;
		pz = s1 * c2 * x + c1 * c2 * z + s2 * y;

		//Apply perspective
		if ((vd > 0) && (vd < Number.POSITIVE_INFINITY)) {
			px = px * (vd / (pz + ze + vd));
			py = py * (vd / (pz + ze + vd));
		}

		//Apply translation
		px = px + xe;
		py = py + ye;
		pz = pz + ze;

		result.push({
			x: (inverted ? py : px),
			y: (inverted ? px : py),
			z: pz
		});
	});
	return result;
}
// Make function acessible to plugins
Highcharts.perspective = perspective;
/*** 
	EXTENSION TO THE SVG-RENDERER TO ENABLE 3D SHAPES
	***/
////// HELPER METHODS //////
var dFactor = (4 * (Math.sqrt(2) - 1) / 3) / (PI / 2);

function defined(obj) {
	return obj !== undefined && obj !== null;
}

//Shoelace algorithm -- http://en.wikipedia.org/wiki/Shoelace_formula
function shapeArea(vertexes) {
	var area = 0,
		i,
		j;
	for (i = 0; i < vertexes.length; i++) {
		j = (i + 1) % vertexes.length;
		area += vertexes[i].x * vertexes[j].y - vertexes[j].x * vertexes[i].y;
	}
	return area / 2;
}

function averageZ(vertexes) {
	var z = 0,
		i;
	for (i = 0; i < vertexes.length; i++) {
		z += vertexes[i].z;
	}
	return vertexes.length ? z / vertexes.length : 0;
}

/** Method to construct a curved path
  * Can 'wrap' around more then 180 degrees
  */
function curveTo(cx, cy, rx, ry, start, end, dx, dy) {
	var result = [];
	if ((end > start) && (end - start > PI / 2 + 0.0001)) {
		result = result.concat(curveTo(cx, cy, rx, ry, start, start + (PI / 2), dx, dy));
		result = result.concat(curveTo(cx, cy, rx, ry, start + (PI / 2), end, dx, dy));
		return result;
	} else if ((end < start) && (start - end > PI / 2 + 0.0001)) {			
		result = result.concat(curveTo(cx, cy, rx, ry, start, start - (PI / 2), dx, dy));
		result = result.concat(curveTo(cx, cy, rx, ry, start - (PI / 2), end, dx, dy));
		return result;
	} else {
		var arcAngle = end - start;
		return [
			'C', 
			cx + (rx * cos(start)) - ((rx * dFactor * arcAngle) * sin(start)) + dx,
			cy + (ry * sin(start)) + ((ry * dFactor * arcAngle) * cos(start)) + dy,
			cx + (rx * cos(end)) + ((rx * dFactor * arcAngle) * sin(end)) + dx,
			cy + (ry * sin(end)) - ((ry * dFactor * arcAngle) * cos(end)) + dy,

			cx + (rx * cos(end)) + dx,
			cy + (ry * sin(end)) + dy
		];
	}
}

Highcharts.SVGRenderer.prototype.toLinePath = function (points, closed) {
	var result = [];

	// Put "L x y" for each point
	Highcharts.each(points, function (point) {
		result.push('L', point.x, point.y);
	});

	if (points.length) {
		// Set the first element to M
		result[0] = 'M';

		// If it is a closed line, add Z
		if (closed) {
			result.push('Z');
		}
	}
	
	return result;
};

////// CUBOIDS //////
Highcharts.SVGRenderer.prototype.cuboid = function (shapeArgs) {

	var result = this.g(),
	paths = this.cuboidPath(shapeArgs);

	// create the 3 sides
	result.front = this.path(paths[0]).attr({zIndex: paths[3], 'stroke-linejoin': 'round'}).add(result);
	result.top = this.path(paths[1]).attr({zIndex: paths[4], 'stroke-linejoin': 'round'}).add(result);
	result.side = this.path(paths[2]).attr({zIndex: paths[5], 'stroke-linejoin': 'round'}).add(result);

	// apply the fill everywhere, the top a bit brighter, the side a bit darker
	result.fillSetter = function (color) {
		var c0 = color,
		c1 = Highcharts.Color(color).brighten(0.1).get(),
		c2 = Highcharts.Color(color).brighten(-0.1).get();

		this.front.attr({fill: c0});
		this.top.attr({fill: c1});
		this.side.attr({fill: c2});

		this.color = color;
		return this;
	};

	// apply opacaity everywhere
	result.opacitySetter = function (opacity) {
		this.front.attr({opacity: opacity});
		this.top.attr({opacity: opacity});
		this.side.attr({opacity: opacity});
		return this;
	};

	result.attr = function (args) {
		if (args.shapeArgs || defined(args.x)) {
			var shapeArgs = args.shapeArgs || args;
			var paths = this.renderer.cuboidPath(shapeArgs);
			this.front.attr({d: paths[0], zIndex: paths[3]});
			this.top.attr({d: paths[1], zIndex: paths[4]});
			this.side.attr({d: paths[2], zIndex: paths[5]});			
		} else {
			Highcharts.SVGElement.prototype.attr.call(this, args);
		}

		return this;
	};
	
	result.animate = function (args, duration, complete) {
		if (defined(args.x) && defined(args.y)) {
			var paths = this.renderer.cuboidPath(args);
			this.front.attr({zIndex: paths[3]}).animate({d: paths[0]}, duration, complete);
			this.top.attr({zIndex: paths[4]}).animate({d: paths[1]}, duration, complete);
			this.side.attr({zIndex: paths[5]}).animate({d: paths[2]}, duration, complete);
		} else if (args.opacity) {				
				this.front.animate(args, duration, complete);
				this.top.animate(args, duration, complete);
				this.side.animate(args, duration, complete);
		} else {
			Highcharts.SVGElement.prototype.animate.call(this, args, duration, complete);
		}
		return this;
	};

	// destroy all children
	result.destroy = function () {
		this.front.destroy();
		this.top.destroy();
		this.side.destroy();

		return null;
	};

	// Apply the Z index to the cuboid group
	result.attr({ zIndex: -paths[3] });

	return result;
};

/**
 *	Generates a cuboid
 */
Highcharts.SVGRenderer.prototype.cuboidPath = function (shapeArgs) {
	var x = shapeArgs.x,
		y = shapeArgs.y,
		z = shapeArgs.z,
		h = shapeArgs.height,
		w = shapeArgs.width,
		d = shapeArgs.depth,		
		chart = Highcharts.charts[this.chartIndex],
		map = Highcharts.map;

	// The 8 corners of the cube
	var pArr = [
		{x: x, y: y, z: z},
		{x: x + w, y: y, z: z},
		{x: x + w, y: y + h, z: z},
		{x: x, y: y + h, z: z},
		{x: x, y: y + h, z: z + d},
		{x: x + w, y: y + h, z: z + d},
		{x: x + w, y: y, z: z + d},
		{x: x, y: y, z: z + d}
	];

	// apply perspective
	pArr = perspective(pArr, chart, shapeArgs.insidePlotArea);

	// helper method to decide which side is visible
	var pickShape = function (path1, path2) {
		path1 = map(path1, function (i) { return pArr[i]; });
		path2 = map(path2, function (i) { return pArr[i]; });
		if (shapeArea(path1) < 0) {
			return path1;
		} else if (shapeArea(path2) < 0) {
			return path2;
		} else {
			return [];
		}
	};

	// front or back
	var front = [3, 2, 1, 0];
	var back = [7, 6, 5, 4];
	var path1 = pickShape(front, back);

	// top or bottom
	var top = [1, 6, 7, 0];
	var bottom = [4, 5, 2, 3];
	var path2 = pickShape(top, bottom);

	// side
	var right = [1, 2, 5, 6];
	var left = [0, 7, 4, 3];
	var path3 = pickShape(right, left);

	return [this.toLinePath(path1, true), this.toLinePath(path2, true), this.toLinePath(path3, true), averageZ(path1), averageZ(path2), averageZ(path3)];
};

////// SECTORS //////
Highcharts.SVGRenderer.prototype.arc3d = function (shapeArgs) {

	shapeArgs.alpha *= deg2rad;
	shapeArgs.beta *= deg2rad;
	var result = this.g(),
		paths = this.arc3dPath(shapeArgs),
		renderer = result.renderer;

	var zIndex = paths.zTop * 100;

	result.shapeArgs = shapeArgs;	// Store for later use

	// create the different sub sections of the shape
	result.top = renderer.path(paths.top).setRadialReference(shapeArgs.center).attr({zIndex: paths.zTop}).add(result);
	result.side1 = renderer.path(paths.side2).attr({zIndex: paths.zSide1});
	result.side2 = renderer.path(paths.side1).attr({zIndex: paths.zSide2});
	result.inn = renderer.path(paths.inn).attr({zIndex: paths.zInn});
	result.out = renderer.path(paths.out).attr({zIndex: paths.zOut});

	// apply the fill to the top and a darker shade to the sides
	result.fillSetter = function (color) {
		this.color = color;

		var c0 = color,
		c2 = Highcharts.Color(color).brighten(-0.1).get();
		
		this.side1.attr({fill: c2});
		this.side2.attr({fill: c2});
		this.inn.attr({fill: c2});
		this.out.attr({fill: c2});
		this.top.attr({fill: c0});
		return this;
	};
	
	// apply the translation to all
	result.translateXSetter = function (value) {
		this.out.attr({translateX: value});
		this.inn.attr({translateX: value});
		this.side1.attr({translateX: value});
		this.side2.attr({translateX: value});
		this.top.attr({translateX: value});
	};
	
	result.translateYSetter = function (value) {
		this.out.attr({translateY: value});
		this.inn.attr({translateY: value});
		this.side1.attr({translateY: value});
		this.side2.attr({translateY: value});
		this.top.attr({translateY: value});
	};

	result.animate = function (args, duration, complete) {
		if (defined(args.end) || defined(args.start)) {
			this._shapeArgs = this.shapeArgs;

			Highcharts.SVGElement.prototype.animate.call(this, {
				_args: args	
			}, {
				duration: duration,
				start: function () {
					var args = arguments,
						fx = args[0],					
						elem = fx.elem,
						end = elem._shapeArgs;

					if (end.fill !== elem.color) {
						elem.attr({
							fill: end.fill
						});
					}
				},
				step: function () {
					var args = arguments,
						fx = args[1],
						result = fx.elem,						
						start = result._shapeArgs,
						end = fx.end,
						pos = fx.pos,
						sA = Highcharts.merge(start, {
							x: start.x + ((end.x - start.x) * pos),
							y: start.y + ((end.y - start.y) * pos),
							r: start.r + ((end.r - start.r) * pos),
							innerR: start.innerR + ((end.innerR - start.innerR) * pos),
							start: start.start + ((end.start - start.start) * pos),
							end: start.end + ((end.end - start.end) * pos)
						});

					var paths = result.renderer.arc3dPath(sA);

					result.shapeArgs = sA;

					result.top.attr({d: paths.top, zIndex: paths.zTop});
					result.inn.attr({d: paths.inn, zIndex: paths.zInn});
					result.out.attr({d: paths.out, zIndex: paths.zOut});
					result.side1.attr({d: paths.side1, zIndex: paths.zSide1});
					result.side2.attr({d: paths.side2, zIndex: paths.zSide2});

				}
			}, complete);
		} else {			
			Highcharts.SVGElement.prototype.animate.call(this, args, duration, complete);
		}
		return this;
	};

	// destroy all children
	result.destroy = function () {
		this.top.destroy();
		this.out.destroy();
		this.inn.destroy();
		this.side1.destroy();
		this.side2.destroy();

		Highcharts.SVGElement.prototype.destroy.call(this);
	};
	// hide all children
	result.hide = function () {
		this.top.hide();
		this.out.hide();
		this.inn.hide();
		this.side1.hide();
		this.side2.hide();
	};
	result.show = function () {
		this.top.show();
		this.out.show();
		this.inn.show();
		this.side1.show();
		this.side2.show();
	};	
	// show all children
	result.zIndex = zIndex;
	result.attr({zIndex: zIndex});
	return result;
};

/**
 * Generate the paths required to draw a 3D arc
 */
Highcharts.SVGRenderer.prototype.arc3dPath = function (shapeArgs) {
	var cx = shapeArgs.x, // x coordinate of the center
		cy = shapeArgs.y, // y coordinate of the center
		start = shapeArgs.start, // start angle
		end = shapeArgs.end - 0.00001, // end angle
		r = shapeArgs.r, // radius
		ir = shapeArgs.innerR, // inner radius
		d = shapeArgs.depth, // depth
		alpha = shapeArgs.alpha, // alpha rotation of the chart
		beta = shapeArgs.beta; // beta rotation of the chart

	// Derived Variables
	var cs = cos(start),		// cosinus of the start angle
		ss = sin(start),		// sinus of the start angle
		ce = cos(end),			// cosinus of the end angle
		se = sin(end),			// sinus of the end angle
		rx = r * cos(beta),		// x-radius 
		ry = r * cos(alpha),	// y-radius
		irx = ir * cos(beta),	// x-radius (inner)
		iry = ir * cos(alpha),	// y-radius (inner)
		dx = d * sin(beta),		// distance between top and bottom in x
		dy = d * sin(alpha);	// distance between top and bottom in y

	// TOP	
	var top = ['M', cx + (rx * cs), cy + (ry * ss)];
	top = top.concat(curveTo(cx, cy, rx, ry, start, end, 0, 0));
	top = top.concat([
		'L', cx + (irx * ce), cy + (iry * se)
	]);
	top = top.concat(curveTo(cx, cy, irx, iry, end, start, 0, 0));
	top = top.concat(['Z']);

	// OUTSIDE
	var b = (beta > 0 ? PI / 2 : 0),
		a = (alpha > 0 ? 0 : PI / 2);

	var start2 = start > -b ? start : (end > -b ? -b : start),
		end2 = end < PI - a ? end : (start < PI - a ? PI - a : end);
	
	var out = ['M', cx + (rx * cos(start2)), cy + (ry * sin(start2))];
	out = out.concat(curveTo(cx, cy, rx, ry, start2, end2, 0, 0));
	
	// When slice goes over middle, need to add both, left and right outer side:
	if (end > PI - a && start < PI - a) {
	// Go to outer side
	out = out.concat([
	'L', cx + (rx * cos(end2)) + dx, cy + (ry * sin(end2)) + dy
	]);
	// Curve to the true end of the slice
	out = out.concat(curveTo(cx, cy, rx, ry, end2, end, dx, dy));
	// Go to the inner side
	out = out.concat([
	'L', cx + (rx * cos(end)), cy + (ry * sin(end))
	]);
	// Go back to the artifical end2
	out = out.concat(curveTo(cx, cy, rx, ry, end, end2, 0, 0));
	}
	
	out = out.concat([
		'L', cx + (rx * cos(end2)) + dx, cy + (ry * sin(end2)) + dy
	]);
	out = out.concat(curveTo(cx, cy, rx, ry, end2, start2, dx, dy));
	out = out.concat(['Z']);

	// INSIDE
	var inn = ['M', cx + (irx * cs), cy + (iry * ss)];
	inn = inn.concat(curveTo(cx, cy, irx, iry, start, end, 0, 0));
	inn = inn.concat([
		'L', cx + (irx * cos(end)) + dx, cy + (iry * sin(end)) + dy
	]);
	inn = inn.concat(curveTo(cx, cy, irx, iry, end, start, dx, dy));
	inn = inn.concat(['Z']);

	// SIDES
	var side1 = [
		'M', cx + (rx * cs), cy + (ry * ss),
		'L', cx + (rx * cs) + dx, cy + (ry * ss) + dy,
		'L', cx + (irx * cs) + dx, cy + (iry * ss) + dy,
		'L', cx + (irx * cs), cy + (iry * ss),
		'Z'
	];
	var side2 = [
		'M', cx + (rx * ce), cy + (ry * se),
		'L', cx + (rx * ce) + dx, cy + (ry * se) + dy,
		'L', cx + (irx * ce) + dx, cy + (iry * se) + dy,
		'L', cx + (irx * ce), cy + (iry * se),
		'Z'
	];
	
	// correction for changed position of vanishing point caused by alpha and beta rotations
	var angleCorr = Math.atan2(dy, -dx),
		angleEnd = Math.abs(end + angleCorr), 
		angleStart = Math.abs(start + angleCorr),
		angleMid = Math.abs((start + end) / 2 + angleCorr);
	
	// set to 0-PI range
	function toZeroPIRange(angle) {
		angle = angle % (2 * PI);
		if (angle > PI) {
			angle = 2 * PI - angle; 
		}
		return angle;
	}
	angleEnd = toZeroPIRange(angleEnd);
	angleStart = toZeroPIRange(angleStart);
	angleMid = toZeroPIRange(angleMid);
	
	// *1e5 is to compensate pInt in zIndexSetter
	var incPrecision = 1e5,
		a1 = angleMid * incPrecision,
		a2 = angleStart * incPrecision,
		a3 = angleEnd * incPrecision;
		
	return {
		top: top,
		zTop: PI * incPrecision + 1, // max angle is PI, so this is allways higher
		out: out,
		zOut: Math.max(a1, a2, a3),
		inn: inn,
		zInn: Math.max(a1, a2, a3),
		side1: side1,
		zSide1: a3 * 0.99, // to keep below zOut and zInn in case of same values
		side2: side2,
		zSide2: a2 * 0.99
	};
};
/*** 
	EXTENSION FOR 3D CHARTS
***/
// Shorthand to check the is3d flag
Highcharts.Chart.prototype.is3d = function () {
	return this.options.chart.options3d && this.options.chart.options3d.enabled; // #4280
};

Highcharts.wrap(Highcharts.Chart.prototype, 'isInsidePlot', function (proceed) {
	if (this.is3d()) {
		return true;
	} else {
		return proceed.apply(this, [].slice.call(arguments, 1));
	}
});

var defaultChartOptions = Highcharts.getOptions();
defaultChartOptions.chart.options3d = {
	enabled: false,
	alpha: 0,
	beta: 0,
	depth: 100,
	viewDistance: 25,
	frame: {
		bottom: { size: 1, color: 'rgba(255,255,255,0)' },
		side: { size: 1, color: 'rgba(255,255,255,0)' },
		back: { size: 1, color: 'rgba(255,255,255,0)' }
	}
};

Highcharts.wrap(Highcharts.Chart.prototype, 'init', function (proceed) {
	var args = [].slice.call(arguments, 1),
		plotOptions,
		pieOptions;

	if (args[0].chart.options3d && args[0].chart.options3d.enabled) {
		// Normalize alpha and beta to (-360, 360) range
		args[0].chart.options3d.alpha = (args[0].chart.options3d.alpha || 0) % 360;
		args[0].chart.options3d.beta = (args[0].chart.options3d.beta || 0) % 360;

		plotOptions = args[0].plotOptions || {};
		pieOptions = plotOptions.pie || {};

		pieOptions.borderColor = Highcharts.pick(pieOptions.borderColor, undefined); 
	}
	proceed.apply(this, args);
});

Highcharts.wrap(Highcharts.Chart.prototype, 'setChartSize', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	if (this.is3d()) {
		var inverted = this.inverted,
			clipBox = this.clipBox,
			margin = this.margin,
			x = inverted ? 'y' : 'x',
			y = inverted ? 'x' : 'y',
			w = inverted ? 'height' : 'width',
			h = inverted ? 'width' : 'height';

		clipBox[x] = -(margin[3] || 0);
		clipBox[y] = -(margin[0] || 0);
		clipBox[w] = this.chartWidth + (margin[3] || 0) + (margin[1] || 0);
		clipBox[h] = this.chartHeight + (margin[0] || 0) + (margin[2] || 0);
	}
});

Highcharts.wrap(Highcharts.Chart.prototype, 'redraw', function (proceed) {
	if (this.is3d()) {
		// Set to force a redraw of all elements
		this.isDirtyBox = true;
	}
	proceed.apply(this, [].slice.call(arguments, 1));	
});

// Draw the series in the reverse order (#3803, #3917)
Highcharts.wrap(Highcharts.Chart.prototype, 'renderSeries', function (proceed) {
	var series,
		i = this.series.length;
	
	if (this.is3d()) {
		while (i--) {		
			series = this.series[i];
			series.translate();
			series.render();	
		}
	} else {
		proceed.call(this);
	}
});

Highcharts.Chart.prototype.retrieveStacks = function (stacking) {
	var series = this.series,
		stacks = {},
		stackNumber,
		i = 1;

	Highcharts.each(this.series, function (s) {
		stackNumber = pick(s.options.stack, (stacking ? 0 : series.length - 1 - s.index)); // #3841, #4532
		if (!stacks[stackNumber]) {
			stacks[stackNumber] = { series: [s], position: i};
			i++;
		} else {
			stacks[stackNumber].series.push(s);
		}
	});

	stacks.totalStacks = i + 1;
	return stacks;
};

/***
	EXTENSION TO THE AXIS
***/
Highcharts.wrap(Highcharts.Axis.prototype, 'setOptions', function (proceed, userOptions) {
	var options;
	proceed.call(this, userOptions);
	if (this.chart.is3d()) {
		options = this.options;
		options.tickWidth = Highcharts.pick(options.tickWidth, 0);
		options.gridLineWidth = Highcharts.pick(options.gridLineWidth, 1);
	}
});

Highcharts.wrap(Highcharts.Axis.prototype, 'render', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return;
	}

	var chart = this.chart,
		renderer = chart.renderer,
		options3d = chart.options.chart.options3d,
		frame = options3d.frame,
		fbottom = frame.bottom,
		fback = frame.back,
		fside = frame.side,
		depth = options3d.depth,
		height = this.height,
		width = this.width,
		left = this.left,
		top = this.top;

	if (this.isZAxis) {
		return;
	} else if (this.horiz) {
		var bottomShape = {
			x: left,
			y: top + (chart.xAxis[0].opposite ? -fbottom.size : height),
			z: 0,
			width: width,
			height: fbottom.size,
			depth: depth,
			insidePlotArea: false
		};
		if (!this.bottomFrame) {
			this.bottomFrame = renderer.cuboid(bottomShape).attr({
				fill: fbottom.color,
				zIndex: (chart.yAxis[0].reversed && options3d.alpha > 0 ? 4 : -1)
			})
			.css({
				stroke: fbottom.color
			}).add();
		} else {
			this.bottomFrame.animate(bottomShape);
		}
	} else {
		// BACK
		var backShape = {
			x: left + (chart.yAxis[0].opposite ? 0 : -fside.size),
			y: top + (chart.xAxis[0].opposite ? -fbottom.size : 0),
			z: depth,
			width: width + fside.size,
			height: height + fbottom.size,
			depth: fback.size,
			insidePlotArea: false
		};
		if (!this.backFrame) {
			this.backFrame = renderer.cuboid(backShape).attr({
				fill: fback.color, 
				zIndex: -3
			}).css({
				stroke: fback.color
			}).add();
		} else {
			this.backFrame.animate(backShape);
		}
		var sideShape = {
			x: left + (chart.yAxis[0].opposite ? width : -fside.size),
			y: top + (chart.xAxis[0].opposite ? -fbottom.size : 0),
			z: 0,
			width: fside.size,
			height: height + fbottom.size,
			depth: depth,
			insidePlotArea: false
		};
		if (!this.sideFrame) {
			this.sideFrame = renderer.cuboid(sideShape).attr({
				fill: fside.color, 
				zIndex: -2
			}).css({
				stroke: fside.color
			}).add();
		} else {
			this.sideFrame.animate(sideShape);
		}
	}
});

Highcharts.wrap(Highcharts.Axis.prototype, 'getPlotLinePath', function (proceed) {
	var path = proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return path;
	}

	if (path === null) { return path; }

	var chart = this.chart,
		options3d = chart.options.chart.options3d;

	var d = this.isZAxis ? this.chart.plotWidth : options3d.depth,
		opposite = this.opposite;
	if (this.horiz) {
		opposite = !opposite;
	}
	var pArr = [
		this.swapZ({ x: path[1], y: path[2], z: (opposite ? d : 0)}),
		this.swapZ({ x: path[1], y: path[2], z: d }),
		this.swapZ({ x: path[4], y: path[5], z: d }),
		this.swapZ({ x: path[4], y: path[5], z: (opposite ? 0 : d)})
	];

	pArr = perspective(pArr, this.chart, false);
	path = this.chart.renderer.toLinePath(pArr, false);

	return path;
});

// Do not draw axislines in 3D
Highcharts.wrap(Highcharts.Axis.prototype, 'getLinePath', function (proceed) {
	return this.chart.is3d() ? [] : proceed.apply(this, [].slice.call(arguments, 1));
});

Highcharts.wrap(Highcharts.Axis.prototype, 'getPlotBandPath', function (proceed) {
	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return proceed.apply(this, [].slice.call(arguments, 1));
	} else {
		var args = arguments,
			from = args[1],
			to = args[2];
	
		var toPath = this.getPlotLinePath(to),
			path = this.getPlotLinePath(from);
			
		if (path && toPath) {
			path.push(
				'L',
				toPath[10],	// These two do not exist in the regular getPlotLine
				toPath[11],  // ---- # 3005
				'L',
				toPath[7],
				toPath[8],
				'L',
				toPath[4],
				toPath[5],
				'L',
				toPath[1],
				toPath[2]
			);
		} else { // outside the axis area
			path = null;
		}
		
		return path;
	}
});

/*** 
	EXTENSION TO THE TICKS
***/

Highcharts.wrap(Highcharts.Tick.prototype, 'getMarkPath', function (proceed) {
	var path = proceed.apply(this, [].slice.call(arguments, 1));	

	// Do not do this if the chart is not 3D
	if (!this.axis.chart.is3d()) {
		return path;
	}

	var pArr = [
		this.axis.swapZ({x: path[1], y: path[2], z: 0}),
		this.axis.swapZ({x: path[4], y: path[5], z: 0})
	];

	pArr = perspective(pArr, this.axis.chart, false);
	path = [
		'M', pArr[0].x, pArr[0].y,
		'L', pArr[1].x, pArr[1].y
	];
	return path;
});

Highcharts.wrap(Highcharts.Tick.prototype, 'getLabelPosition', function (proceed) {
	var pos = proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.axis.chart.is3d()) {
		return pos;
	}

	var new_pos = perspective([this.axis.swapZ({x: pos.x, y: pos.y, z: 0})], this.axis.chart, false)[0];
	new_pos.x = new_pos.x - (!this.axis.horiz && this.axis.opposite ? this.axis.transA : 0); //#3788
	new_pos.old = pos;
	return new_pos;
});

Highcharts.wrap(Highcharts.Tick.prototype, 'handleOverflow', function (proceed, xy) {
	if (this.axis.chart.is3d()) {
		xy = xy.old;
	}
	return proceed.call(this, xy);
});

Highcharts.wrap(Highcharts.Axis.prototype, 'getTitlePosition', function (proceed) {
	var pos = proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return pos;
	}

	pos = perspective([this.swapZ({x: pos.x, y: pos.y, z: 0})], this.chart, false)[0];
	return pos;
});

Highcharts.wrap(Highcharts.Axis.prototype, 'drawCrosshair', function (proceed) {
	var args = arguments;
	if (this.chart.is3d()) {
		if (args[2]) {
			args[2] = {
				plotX: args[2].plotXold || args[2].plotX,
				plotY: args[2].plotYold || args[2].plotY
			};
		}
	}
	proceed.apply(this, [].slice.call(args, 1));
});

/***
    Z-AXIS
***/

Highcharts.Axis.prototype.swapZ = function (p, insidePlotArea) {
	if (this.isZAxis) {
		var plotLeft = insidePlotArea ? 0 : this.chart.plotLeft;
		var chart = this.chart;
		return {
			x: plotLeft + (chart.yAxis[0].opposite ? p.z : chart.xAxis[0].width - p.z),
			y: p.y,
			z: p.x - plotLeft
		};
	} else {
		return p;
	}
};

var ZAxis = Highcharts.ZAxis = function () {
	this.isZAxis = true;
	this.init.apply(this, arguments);
};
Highcharts.extend(ZAxis.prototype, Highcharts.Axis.prototype);
Highcharts.extend(ZAxis.prototype, {
	setOptions: function (userOptions) {
		userOptions = Highcharts.merge({
			offset: 0,
			lineWidth: 0
		}, userOptions);
		Highcharts.Axis.prototype.setOptions.call(this, userOptions);
		this.coll = 'zAxis';
	},
	setAxisSize: function () {
		Highcharts.Axis.prototype.setAxisSize.call(this);
		this.width = this.len = this.chart.options.chart.options3d.depth;
		this.right = this.chart.chartWidth - this.width - this.left;
	},
	getSeriesExtremes: function () {
		var axis = this,
			chart = axis.chart;

		axis.hasVisibleSeries = false;

		// Reset properties in case we're redrawing (#3353)
		axis.dataMin = axis.dataMax = axis.ignoreMinPadding = axis.ignoreMaxPadding = null;
		
		if (axis.buildStacks) {
			axis.buildStacks();
		}

		// loop through this axis' series
		Highcharts.each(axis.series, function (series) {

			if (series.visible || !chart.options.chart.ignoreHiddenSeries) {

				var seriesOptions = series.options,
					zData,
					threshold = seriesOptions.threshold;

				axis.hasVisibleSeries = true;

				// Validate threshold in logarithmic axes
				if (axis.isLog && threshold <= 0) {
					threshold = null;
				}

				zData = series.zData;
				if (zData.length) {
					axis.dataMin = Math.min(pick(axis.dataMin, zData[0]), Math.min.apply(null, zData));
					axis.dataMax = Math.max(pick(axis.dataMax, zData[0]), Math.max.apply(null, zData));
				}
			}
		});
	}
});


/**
* Extend the chart getAxes method to also get the color axis
*/
Highcharts.wrap(Highcharts.Chart.prototype, 'getAxes', function (proceed) {
	var chart = this,
		options = this.options,
		zAxisOptions = options.zAxis = Highcharts.splat(options.zAxis || {});

	proceed.call(this);

	if (!chart.is3d()) {
		return;
	}
	this.zAxis = [];
	Highcharts.each(zAxisOptions, function (axisOptions, i) {
		axisOptions.index = i;
		axisOptions.isX = true; //Z-Axis is shown horizontally, so it's kind of a X-Axis
		var zAxis = new ZAxis(chart, axisOptions);
		zAxis.setScale();
	});
});
/***
	EXTENSION FOR 3D COLUMNS
***/
Highcharts.wrap(Highcharts.seriesTypes.column.prototype, 'translate', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {  
		return;
	}	

	var series = this,
		chart = series.chart,
		seriesOptions = series.options,
		depth = seriesOptions.depth || 25;

	var stack = seriesOptions.stacking ? (seriesOptions.stack || 0) : series._i;
	var z = stack * (depth + (seriesOptions.groupZPadding || 1));

	if (seriesOptions.grouping !== false) { z = 0; }

	z += (seriesOptions.groupZPadding || 1);

	Highcharts.each(series.data, function (point) {
		if (point.y !== null) {
			var shapeArgs = point.shapeArgs,
				tooltipPos = point.tooltipPos;

			point.shapeType = 'cuboid';
			shapeArgs.z = z;
			shapeArgs.depth = depth;
			shapeArgs.insidePlotArea = true;

			// Translate the tooltip position in 3d space
			tooltipPos = perspective([{ x: tooltipPos[0], y: tooltipPos[1], z: z }], chart, false)[0];
			point.tooltipPos = [tooltipPos.x, tooltipPos.y];
		}
	});
	// store for later use #4067
	series.z = z;
});

Highcharts.wrap(Highcharts.seriesTypes.column.prototype, 'animate', function (proceed) {
	if (!this.chart.is3d()) {
		proceed.apply(this, [].slice.call(arguments, 1));
	} else {
		var args = arguments,
			init = args[1],
			yAxis = this.yAxis,
			series = this,
			reversed = this.yAxis.reversed;

		if (Highcharts.svg) { // VML is too slow anyway
			if (init) {
				Highcharts.each(series.data, function (point) {
					if (point.y !== null) {
						point.height = point.shapeArgs.height;
						point.shapey = point.shapeArgs.y;	//#2968				
						point.shapeArgs.height = 1;
						if (!reversed) {
							if (point.stackY) {
								point.shapeArgs.y = point.plotY + yAxis.translate(point.stackY);
							} else {
								point.shapeArgs.y = point.plotY + (point.negative ? -point.height : point.height);
							}
						}
					}
				});

			} else { // run the animation				
				Highcharts.each(series.data, function (point) {					
					if (point.y !== null) {
						point.shapeArgs.height = point.height;
						point.shapeArgs.y = point.shapey;	//#2968
						// null value do not have a graphic
						if (point.graphic) {
							point.graphic.animate(point.shapeArgs, series.options.animation);					
						}
					}
				});

				// redraw datalabels to the correct position
				this.drawDataLabels();

				// delete this function to allow it only once
				series.animate = null;
			}
		}
	}
});

Highcharts.wrap(Highcharts.seriesTypes.column.prototype, 'init', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	if (this.chart.is3d()) {
		var seriesOptions = this.options,	
			grouping = seriesOptions.grouping,
			stacking = seriesOptions.stacking,
			z = 0;	
		
		if (!(grouping !== undefined && !grouping)) {
			var stacks = this.chart.retrieveStacks(stacking),
				stack = seriesOptions.stack || 0,
				i; // position within the stack
			for (i = 0; i < stacks[stack].series.length; i++) {
				if (stacks[stack].series[i] === this) {
					break;
				}
			}
			z = (stacks.totalStacks * 10) - (10 * (stacks.totalStacks - stacks[stack].position)) - i;
		}
				
		seriesOptions.zIndex = z;
	}
});
function draw3DPoints(proceed) {
	// Do not do this if the chart is not 3D
	if (this.chart.is3d()) {		
		var grouping = this.chart.options.plotOptions.column.grouping;
		if (grouping !== undefined && !grouping && this.group.zIndex !== undefined && !this.zIndexSet) {
			this.group.attr({zIndex : (this.group.zIndex * 10)});
			this.zIndexSet = true; // #4062 set zindex only once
		} 

		var options = this.options,
			states = this.options.states;
			
		this.borderWidth = options.borderWidth = defined(options.edgeWidth) ? options.edgeWidth : 1; //#4055

		Highcharts.each(this.data, function (point) {
			if (point.y !== null) {
				var pointAttr = point.pointAttr;

				// Set the border color to the fill color to provide a smooth edge
				this.borderColor = Highcharts.pick(options.edgeColor, pointAttr[''].fill);

				pointAttr[''].stroke = this.borderColor;
				pointAttr.hover.stroke = Highcharts.pick(states.hover.edgeColor, this.borderColor);
				pointAttr.select.stroke = Highcharts.pick(states.select.edgeColor, this.borderColor);
			}
		});
	}

	proceed.apply(this, [].slice.call(arguments, 1));
}

Highcharts.wrap(Highcharts.Series.prototype, 'alignDataLabel', function (proceed) {
	
	// Only do this for 3D columns and columnranges
	if (this.chart.is3d() && (this.type === 'column' || this.type === 'columnrange')) {
		var series = this,
			chart = series.chart;

		var args = arguments,
			alignTo = args[4];
		
		var pos = ({x: alignTo.x, y: alignTo.y, z: series.z});
		pos = perspective([pos], chart, true)[0];
		alignTo.x = pos.x;
		alignTo.y = pos.y;
	}

	proceed.apply(this, [].slice.call(arguments, 1));
});

if (Highcharts.seriesTypes.columnrange) {
	Highcharts.wrap(Highcharts.seriesTypes.columnrange.prototype, 'drawPoints', draw3DPoints);
}

Highcharts.wrap(Highcharts.seriesTypes.column.prototype, 'drawPoints', draw3DPoints);

/*** 
	EXTENSION FOR 3D CYLINDRICAL COLUMNS
	Not supported
***/
/*
var defaultOptions = Highcharts.getOptions();
defaultOptions.plotOptions.cylinder = Highcharts.merge(defaultOptions.plotOptions.column);
var CylinderSeries = Highcharts.extendClass(Highcharts.seriesTypes.column, {
	type: 'cylinder'
});
Highcharts.seriesTypes.cylinder = CylinderSeries;

Highcharts.wrap(Highcharts.seriesTypes.cylinder.prototype, 'translate', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return;
	}	

	var series = this,
		chart = series.chart,
		options = chart.options,
		cylOptions = options.plotOptions.cylinder,
		options3d = options.chart.options3d,
		depth = cylOptions.depth || 0,
		origin = {
			x: chart.inverted ? chart.plotHeight / 2 : chart.plotWidth / 2,
			y: chart.inverted ? chart.plotWidth / 2 : chart.plotHeight / 2, 
			z: options3d.depth,
			vd: options3d.viewDistance
		},
		alpha = options3d.alpha;

	var z = cylOptions.stacking ? (this.options.stack || 0) * depth : series._i * depth;
	z += depth / 2;

	if (cylOptions.grouping !== false) { z = 0; }

	Highcharts.each(series.data, function (point) {
		var shapeArgs = point.shapeArgs;
		point.shapeType = 'arc3d';
		shapeArgs.x += depth / 2;
		shapeArgs.z = z;
		shapeArgs.start = 0;
		shapeArgs.end = 2 * PI;
		shapeArgs.r = depth * 0.95;
		shapeArgs.innerR = 0;
		shapeArgs.depth = shapeArgs.height * (1 / sin((90 - alpha) * deg2rad)) - z;
		shapeArgs.alpha = 90 - alpha;
		shapeArgs.beta = 0;
		shapeArgs.origin = origin;	
	});
});
*//*** 
	EXTENSION FOR 3D PIES
***/

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype, 'translate', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));

	// Do not do this if the chart is not 3D
	if (!this.chart.is3d()) {
		return;
	}	
	
	var series = this,
		chart = series.chart,
		options = chart.options,
		seriesOptions = series.options,
		depth = seriesOptions.depth || 0,
		options3d = options.chart.options3d,
		origin = {
			x: chart.plotWidth / 2,
			y: chart.plotHeight / 2,
			z: options3d.depth
		},
		alpha = options3d.alpha,
		beta = options3d.beta,
		z = seriesOptions.stacking ? (seriesOptions.stack || 0) * depth : series._i * depth;

	z += depth / 2;

	if (seriesOptions.grouping !== false) { z = 0; }

	Highcharts.each(series.data, function (point) {

		var shapeArgs = point.shapeArgs,
			angle;

		point.shapeType = 'arc3d';

		shapeArgs.z = z;
		shapeArgs.depth = depth * 0.75;
		shapeArgs.origin = origin;
		shapeArgs.alpha = alpha;
		shapeArgs.beta = beta;
		shapeArgs.center = series.center;
		
		angle = (shapeArgs.end + shapeArgs.start) / 2;

		point.slicedTranslation = {
			translateX : round(cos(angle) * seriesOptions.slicedOffset * cos(alpha * deg2rad)),
			translateY : round(sin(angle) * seriesOptions.slicedOffset * cos(alpha * deg2rad))
		};
	});
});

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype.pointClass.prototype, 'haloPath', function (proceed) {
	var args = arguments;
	return this.series.chart.is3d() ? [] : proceed.call(this, args[1]);
});

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype, 'drawPoints', function (proceed) {

	var seriesGroup = this.group,
		options = this.options,
		states = options.states;

	// Do not do this if the chart is not 3D
	if (this.chart.is3d()) {
		// Set the border color to the fill color to provide a smooth edge
		this.borderWidth = options.borderWidth = options.edgeWidth || 1;
		this.borderColor = options.edgeColor = Highcharts.pick(options.edgeColor, options.borderColor, undefined);

		states.hover.borderColor = Highcharts.pick(states.hover.edgeColor, this.borderColor);		
		states.hover.borderWidth = Highcharts.pick(states.hover.edgeWidth, this.borderWidth);	
		states.select.borderColor = Highcharts.pick(states.select.edgeColor, this.borderColor);		
		states.select.borderWidth = Highcharts.pick(states.select.edgeWidth, this.borderWidth);

		Highcharts.each(this.data, function (point) {
			var pointAttr = point.pointAttr;
			pointAttr[''].stroke = point.series.borderColor || point.color;
			pointAttr['']['stroke-width'] = point.series.borderWidth;
			pointAttr.hover.stroke = states.hover.borderColor;	
			pointAttr.hover['stroke-width'] = states.hover.borderWidth;
			pointAttr.select.stroke = states.select.borderColor;
			pointAttr.select['stroke-width'] = states.select.borderWidth;
		});	
	}

	proceed.apply(this, [].slice.call(arguments, 1));

	if (this.chart.is3d()) {		
		Highcharts.each(this.points, function (point) {
			var graphic = point.graphic;

			graphic.out.add(seriesGroup);
			graphic.inn.add(seriesGroup);
			graphic.side1.add(seriesGroup);
			graphic.side2.add(seriesGroup);

			// Hide null or 0 points (#3006, 3650)
			graphic[point.y ? 'show' : 'hide']();
		});		
	}
});

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype, 'drawDataLabels', function (proceed) {
	if (this.chart.is3d()) {
		var series = this;
		Highcharts.each(series.data, function (point) {
			var shapeArgs = point.shapeArgs,
				r = shapeArgs.r,
				d = shapeArgs.depth,
				a1 = (shapeArgs.alpha || series.chart.options.chart.options3d.alpha) * deg2rad, //#3240 issue with datalabels for 0 and null values
				a2 = (shapeArgs.start + shapeArgs.end) / 2,
				labelPos = point.labelPos;

			labelPos[1] += (-r * (1 - cos(a1)) * sin(a2)) + (sin(a2) > 0 ? sin(a1) * d : 0);
			labelPos[3] += (-r * (1 - cos(a1)) * sin(a2)) + (sin(a2) > 0 ? sin(a1) * d : 0);
			labelPos[5] += (-r * (1 - cos(a1)) * sin(a2)) + (sin(a2) > 0 ? sin(a1) * d : 0);

		});
	} 

	proceed.apply(this, [].slice.call(arguments, 1));
});

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype, 'addPoint', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));	
	if (this.chart.is3d()) {
		// destroy (and rebuild) everything!!!
		this.update(this.userOptions, true); // #3845 pass the old options
	}
});

Highcharts.wrap(Highcharts.seriesTypes.pie.prototype, 'animate', function (proceed) {
	if (!this.chart.is3d()) {
		proceed.apply(this, [].slice.call(arguments, 1));
	} else {
		var args = arguments,
			init = args[1],
			animation = this.options.animation,
			attribs,
			center = this.center,
			group = this.group,
			markerGroup = this.markerGroup;

		if (Highcharts.svg) { // VML is too slow anyway
				
				if (animation === true) {
					animation = {};
				}
				// Initialize the animation
				if (init) {
				
					// Scale down the group and place it in the center
					group.oldtranslateX = group.translateX;
					group.oldtranslateY = group.translateY;
					attribs = {
						translateX: center[0],
						translateY: center[1],
						scaleX: 0.001, // #1499
						scaleY: 0.001
					};
					
					group.attr(attribs);
					if (markerGroup) {
						markerGroup.attrSetters = group.attrSetters;
						markerGroup.attr(attribs);
					}
				
				// Run the animation
				} else {
					attribs = {
						translateX: group.oldtranslateX,
						translateY: group.oldtranslateY,
						scaleX: 1,
						scaleY: 1
					};
					group.animate(attribs, animation);

					if (markerGroup) {
						markerGroup.animate(attribs, animation);
					}
				
					// Delete this function to allow it only once
					this.animate = null;
				}
				
		}
	}
});/*** 
	EXTENSION FOR 3D SCATTER CHART
***/

Highcharts.wrap(Highcharts.seriesTypes.scatter.prototype, 'translate', function (proceed) {
//function translate3d(proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));
	
	if (!this.chart.is3d()) {
		return;
	}	

	var series = this,
		chart = series.chart,
		zAxis = Highcharts.pick(series.zAxis, chart.options.zAxis[0]),
		rawPoints = [],
		rawPoint,
		projectedPoints,
		projectedPoint,
		zValue,
		i;

	for (i = 0; i < series.data.length; i++) {
		rawPoint = series.data[i];
		zValue = zAxis.isLog && zAxis.val2lin ? zAxis.val2lin(rawPoint.z) : rawPoint.z; // #4562
		rawPoint.plotZ = zAxis.translate(zValue);

		rawPoint.isInside = rawPoint.isInside ? (zValue >= zAxis.min && zValue <= zAxis.max) : false;

		rawPoints.push({
			x: rawPoint.plotX,
			y: rawPoint.plotY,
			z: rawPoint.plotZ
		});
	}

	projectedPoints = perspective(rawPoints, chart, true);

	for (i = 0; i < series.data.length; i++) {
		rawPoint = series.data[i];
		projectedPoint = projectedPoints[i];

		rawPoint.plotXold = rawPoint.plotX;
		rawPoint.plotYold = rawPoint.plotY;

		rawPoint.plotX = projectedPoint.x;
		rawPoint.plotY = projectedPoint.y;
		rawPoint.plotZ = projectedPoint.z;


	}

});

Highcharts.wrap(Highcharts.seriesTypes.scatter.prototype, 'init', function (proceed, chart, options) {
	if (chart.is3d()) {
		// add a third coordinate
		this.axisTypes = ['xAxis', 'yAxis', 'zAxis'];
		this.pointArrayMap = ['x', 'y', 'z'];
		this.parallelArrays = ['x', 'y', 'z'];
	}

	var result = proceed.apply(this, [chart, options]);

	if (this.chart.is3d()) {
		// Set a new default tooltip formatter
		var default3dScatterTooltip = 'x: <b>{point.x}</b><br/>y: <b>{point.y}</b><br/>z: <b>{point.z}</b><br/>';
		if (this.userOptions.tooltip) {
			this.tooltipOptions.pointFormat = this.userOptions.tooltip.pointFormat || default3dScatterTooltip;
		} else {
			this.tooltipOptions.pointFormat = default3dScatterTooltip;
		}
	}
	return result;
});
/**
 *	Extension to the VML Renderer
 */
if (Highcharts.VMLRenderer) {

Highcharts.setOptions({animate: false});

Highcharts.VMLRenderer.prototype.cuboid = Highcharts.SVGRenderer.prototype.cuboid;
Highcharts.VMLRenderer.prototype.cuboidPath = Highcharts.SVGRenderer.prototype.cuboidPath;

Highcharts.VMLRenderer.prototype.toLinePath = Highcharts.SVGRenderer.prototype.toLinePath;

Highcharts.VMLRenderer.prototype.createElement3D = Highcharts.SVGRenderer.prototype.createElement3D;

Highcharts.VMLRenderer.prototype.arc3d = function (shapeArgs) { 
	var result = Highcharts.SVGRenderer.prototype.arc3d.call(this, shapeArgs);
	result.css({zIndex: result.zIndex});
	return result;
};

Highcharts.VMLRenderer.prototype.arc3dPath = Highcharts.SVGRenderer.prototype.arc3dPath;

Highcharts.wrap(Highcharts.Axis.prototype, 'render', function (proceed) {
	proceed.apply(this, [].slice.call(arguments, 1));
	// VML doesn't support a negative z-index
	if (this.sideFrame) {
		this.sideFrame.css({zIndex: 0});
		this.sideFrame.front.attr({fill: this.sideFrame.color});
	}
	if (this.bottomFrame) {
		this.bottomFrame.css({zIndex: 1});
		this.bottomFrame.front.attr({fill: this.bottomFrame.color});
	}	
	if (this.backFrame) {
		this.backFrame.css({zIndex: 0});
		this.backFrame.front.attr({fill: this.backFrame.color});
	}		
});

}

}(Highcharts));
