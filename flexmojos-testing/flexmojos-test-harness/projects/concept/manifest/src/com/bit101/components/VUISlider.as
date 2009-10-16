/**
 * VUISlider.as
 * Keith Peters
 * version 0.97
 * 
 * A vertical Slider with a label and value label.
 * 
 * Copyright (c) 2009 Keith Peters
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
 
package com.bit101.components
{
	import flash.display.DisplayObjectContainer;
	import flash.events.Event;

	public class VUISlider extends UISlider
	{
		
		
		/**
		 * Constructor
		 * @param parent The parent DisplayObjectContainer on which to add this VUISlider.
		 * @param xpos The x position to place this component.
		 * @param ypos The y position to place this component.
		 * @param label The string to use as the label for this component.
		 * @param defaultHandler The event handling function to handle the default event for this component.
		 */
		public function VUISlider(parent:DisplayObjectContainer = null, x:Number = 0, y:Number = 0, label:String = "", defaultEventHandler:Function = null)
		{
			_sliderClass = VSlider;
			super(parent, x, y, label, defaultEventHandler);
		}
		
		/**
		 * Initializes this component.
		 */
		protected override function init():void
		{
			super.init();
			setSize(20, 146);
		}
		
		
		
		
		///////////////////////////////////
		// public methods
		///////////////////////////////////
		
		override public function draw():void
		{
			super.draw();
			_label.x = width / 2 - _label.width / 2;
			
			_slider.x = width / 2 - _slider.width / 2;
			_slider.y = _label.height + 5;
			_slider.height = height - _label.height - _valueLabel.height - 10;
			
			_valueLabel.x = width / 2 - _valueLabel.width / 2;
			_valueLabel.y = _slider.y + _slider.height + 5;
		}
		
		override protected function positionLabel():void
		{
			_valueLabel.x = width / 2 - _valueLabel.width / 2;
		}
		
		
		
		
		///////////////////////////////////
		// event handlers
		///////////////////////////////////
		
		///////////////////////////////////
		// getter/setters
		///////////////////////////////////
		
		override public function get width():Number
		{
			if(_label == null) return _width;
			return Math.max(_width, _label.width);
		}
		
	}
}