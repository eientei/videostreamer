package yukkuplayer.controls;

import flash.display.Sprite;

class VolumeSlider extends Sprite {
    private var m_width:Float;
    private var m_height:Float;
    private var m_filled:Float;

    public function new(x:Float, y:Float, width:Float, height:Float) {
        super();
        this.x = x;
        this.y = y;
        this.buttonMode = true;
        this.useHandCursor = true;
        this.tabEnabled = false;

        m_width = width;
        m_height = height;
        m_filled = 1.0;

        draw();
    }

    public function setPosition(x:Float, y:Float):Void {
        this.x = x;
        this.y = y;

        draw();
    }

    public function setFilled(m:Float):Void {
        m_filled = m;

        draw();
    }

    public function setSize(width:Float, height:Float):Void {
        m_width = width;
        m_height = height;

        draw();
    }

    private function draw():Void {
        graphics.clear();

        graphics.lineStyle(4, 0xffffff);
        graphics.beginFill(0xaaaaaa, 1);
        graphics.drawRect(0, 0, m_width, m_height);
        graphics.endFill();
        graphics.beginFill(0xffffff, 1);
        graphics.drawRect(0, 0, m_width * m_filled, m_height);
        graphics.endFill();
    }
}