package com.zombierush.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;

/**
 * Main game play screen
 */
public class PlayGame extends InputAdapter implements Screen {
    
    // A reference to the main game
    ZombieRush game;
    
    // Camera reference
    OrthographicCamera camera;
    
    // Sprite batch reference
    SpriteBatch batch;
    
    // Counting FPS
    FPSLogger fpsLogger = new FPSLogger();
    
    // FPS counter
    int fps = 0;
    int numFrames = 0;
    float deltaT = 0.0f;
    
    // Textures for the HUD
    Array <GenericEntity> entities;
    GenericEntity selectedHumanHUD;
    
    // Zombie timer
    float timeSinceLastZombieSpawn = 0.0f;
    float timeSinceLastZombieCheck = 0;
    final float ZOMBIE_SPAWN_RATE = 3.0f;

    /**
     * Default constructor
     */
    public PlayGame(ZombieRush g)
    {
        game        = g;
        camera      = game.GetCamera();
        batch       = game.GetBatch();
        Gdx.input.setInputProcessor(this);
        
        game.zombies                = new Array();
        game.barricades             = new Array();
        game.shapeEffects           = new Array();
        entities                    = new Array();
        game.humans                 = new Array();
        game.humans.add(new Human(game.humanTex, g));
        game.humans.add(new Human(game.humanTex, g));
        game.selectedHuman = new Human();
        
        // Create HUD textures
        GenericEntity ge = new GenericEntity(game.barricadeTex);
        ge.sprite.setSize(game.SCREEN_WIDTH, 100);
        ge.xPosition = game.SCREEN_WIDTH/2;
        ge.yPosition = 50;
        entities.add(ge);
        selectedHumanHUD = new GenericEntity(game.humanTex);
        selectedHumanHUD.xPosition = 100;
        selectedHumanHUD.yPosition = 60;
        selectedHumanHUD.sprite.setSize(45, 45);
        
        // Reset game variables
        game.points = 0;
        game.bucks = 0;
    }
    
    /**
     * Show the menu
     */
    @Override
    public void show()
    {
        
    }  // end show
    
    /**
     * Render this menu
     * @param delta Elapsed time in seconds
     */
    public void render(float delta)
    {
        // Count our FPS
        numFrames++;
        deltaT += delta;
        if (deltaT > 1.0f)
        {
            fps = numFrames;
            deltaT = 0.0f;
            numFrames = 0;
        }
        
        // Update the game
        Update(delta);
        
        // Update the camera
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        game.shapeRenderer.setProjectionMatrix(camera.combined);
        
        Gdx.gl.glClearColor(0, 1, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        batch.begin();
        RenderBackground();
        RenderHuman();
        RenderZombies();
        RenderBarricades();
        RenderHUD();
        
        game.GetFont().draw(game.GetBatch(), "FPS: " + fps, 10, 80);
        
        batch.end();
        
        RenderShapeEffects();
        RenderHighlight();
        RenderNightOverlay();
    }
    
    /**
     * Update all entities
     * @param delta 
     */
    private void Update(float delta)
    {
        /**
         * Update in game time
         */
        game.timeOfDay += delta;
        if (game.timeOfDay > game.MAX_TIME_OF_DAY)
            game.timeOfDay -= game.MAX_TIME_OF_DAY;
        
        // Spawn new zombies as appropriate
        if (game.timeOfDay < game.MAX_TIME_OF_DAY * 0.25 || game.timeOfDay > game.MAX_TIME_OF_DAY * 0.75)
        {
            timeSinceLastZombieCheck += delta;
            if (timeSinceLastZombieCheck > 1)
            {
                timeSinceLastZombieSpawn += delta;
                float rnd = MathUtils.random(ZOMBIE_SPAWN_RATE);
                
                if (rnd < timeSinceLastZombieSpawn)
                {
                    game.zombies.add(new Zombie(game.zombieTex, game));
                    timeSinceLastZombieSpawn = 0;
                }
                
                timeSinceLastZombieCheck = 0;
            } 
        }
        else
        {
            timeSinceLastZombieSpawn = 0;
        }
        
        /**
         * Update all game elements
         */
        for (Human h : game.humans)
        {
            h.Update(delta);
        }
        for (Zombie z : game.zombies)
        {
            z.Update(delta);
        }
        for (Barricade b : game.barricades)
        {
            b.Update(delta);
        }
        for (ShapeEffect e : game.shapeEffects)
        {
            e.Update(delta);
        }
        
        /**
         * Remove any dead zombies
         */
        Array <Zombie> removeList = new Array();
        for (Zombie z : game.zombies)
        {
            if (z.health <= 0)
            {
                removeList.add(z);
                game.points++;
                game.bucks++;
            }
        }
        game.zombies.removeAll(removeList, true);
        
        /**
         * Check for dead humans
         */
        for (Human h : game.humans)
        {
            if (h.health <= 0)
            {
                System.out.println("Human is DEAD!!!");
            }
        }
        
        /**
         * Remove any dead barricades
         */
        Array <Barricade> removeListB = new Array();
        for (Barricade b : game.barricades)
        {
            if (b.health <= 0)
                removeListB.add(b);
        }
        game.barricades.removeAll(removeListB, true);
        
        /**
         * Remove finished shape effects
         */
        Array <ShapeEffect> removeListE = new Array();
        for (ShapeEffect e : game.shapeEffects)
        {
            if (e.timeToLive <= 0)
                removeListE.add(e);
        }
        game.shapeEffects.removeAll(removeListE, true);
        

        
        
    }  // end Update
    
    /**
     * Draw the background to the screen
     */
    private void RenderBackground()
    {
        batch.disableBlending();
        for (int x = 0; x < 800;)
        {
            for (int y = 600; y > -128;)
            {
                batch.draw(game.tileTex, x, y);
                y -= 128;
            }
            x += 128;
        }
        batch.enableBlending();
        
    }  // end RenderBackground
    
    /**
     * Render all human characters
     */
    private void RenderHuman()
    {
        for (Human h : game.humans)
        {
            h.Render(batch);
        }
        
    }  // end RenderHuman
    
    /**
     * Render all zombies to the screen
     */
    private void RenderZombies()
    {
        for (Zombie z : game.zombies)
        {
            z.Render(batch);
        }
    }
    
    /**
     * Render all barricades to the screen
     */
    private void RenderBarricades()
    {
        for(Barricade b : game.barricades)
        {
            b.Render(batch);
        }
    }
    
    /**
     * Render the cursor highlight to assist building
     */
    private void RenderHighlight()
    {
        if (game.clickState != game.BUILD)
            return;
        
        // Get mouse position
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        y = game.SCREEN_HEIGHT - y;
        x = (int)((x) / 40 * 40);
        y = (int)((y) / 40 * 40);
        
        // Don't overlap the HUD
        if (y < 100)
            return;
        
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.setColor(0.5f, 1.0f, 0.5f, 0.5f);
        game.shapeRenderer.rect(x, y, 40, 40);
        game.shapeRenderer.end();
    }
    
    /**
     * Render the transparent overlay that makes it night
     */
    private void RenderNightOverlay()
    {
        float alpha = 0.0f;
        float t = Math.abs(game.timeOfDay - (game.MAX_TIME_OF_DAY / 2));
        if (t > game.MAX_TIME_OF_DAY * game.MAX_STOP)
        {
            alpha = game.MAX_ALPHA;
        }   
        else if (t < game.MAX_TIME_OF_DAY * game.MIN_STOP)
        {
            alpha = 0;
        }
        else
        {
            t -= (game.MAX_TIME_OF_DAY * game.MIN_STOP);
            t = t / (game.MAX_TIME_OF_DAY * game.MAX_STOP - game.MAX_TIME_OF_DAY * game.MIN_STOP);
            alpha = game.MAX_ALPHA * t;
        }
        
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.shapeRenderer.setColor(0, 0.2f, 0.6f, alpha);
        game.shapeRenderer.rect(0, 100, game.SCREEN_WIDTH, game.SCREEN_HEIGHT);
        game.shapeRenderer.end();
    }
    
    /**
     * Render all shape effects to the screen
     */
    private void RenderShapeEffects()
    {
        for(ShapeEffect e : game.shapeEffects)
        {
            e.Render(batch);
        }
    }
    
    /**
     * Render everything to create our HUD
     */
    private void RenderHUD()
    {
        for (GenericEntity g : entities)
        {
            g.Render(batch);
        }
        
        // Points
        game.GetFont().draw(game.GetBatch(), "Points: " + game.points, 200, 80);
        
        // Bucks
        game.GetFont().draw(game.GetBatch(), "Bucks: " + game.bucks, 300, 80);
        
        // Selected human
        if (game.selectedHuman.isValid)
        {
            selectedHumanHUD.Render(batch);
            game.GetFont().draw(game.GetBatch(), (int)(game.selectedHuman.health*10) + "%", 80, 20);
        }
        
        // Night/day clock
        // todo
        
    }
        
    /**
     * Handle mouse clicks
     */
    public boolean touchUp (int x, int y, int pointer, int button) 
    {
        // Modify touch coordinates to match render axis
        y = 600 - y;
        
        if (button == Input.Buttons.LEFT && game.clickState == game.NORMAL)
        {
            // See if we are selecting a new human
            for (Human h : game.humans)
            {
                Rectangle r = new Rectangle(x, y, 1, 1);
                if (r.overlaps(h.sprite.getBoundingRectangle()))
                {
                    game.selectedHuman = h;
                }
            }
        }        
        else if (button == Input.Buttons.RIGHT && game.clickState == game.NORMAL && game.selectedHuman.isValid)
        {
            // Update human's desired position to be the current click
            game.selectedHuman.UpdateDesiredPosition(x, y);
        }
        else if (button == Input.Buttons.RIGHT && game.clickState == game.BUILD && game.selectedHuman.isValid)
        {
            /**
             * Barricades must be locked to a grid
             */
            x = (int)((x) / 40 * 40);
            y = (int)((y) / 40 * 40);
            x += 20;
            y += 20;
            
            /**
             * Don't build multiple barricades in the same spot
             */
            for (Barricade b : game.barricades)
            {
                if (b.xPosition == x && b.yPosition == y)
                    return true;
            }
            
            game.barricades.add(new Barricade(game.barricadeTex, x, y, game.selectedHuman));
            game.selectedHuman.UpdateDesiredPosition(x, y);
            
            game.clickState = game.NORMAL;  // Switch back to normal click mode
        }
        
        return true;
        
    }  // end touchUp
    
    /**
     * Handle keyboard events
     */
    public boolean keyUp (int keycode) 
    {
        if (keycode == Input.Keys.Z)
        {
            // Spawn a new zombie!
            game.zombies.add(new Zombie(game.zombieTex, game));
        }
        else if (keycode == Input.Keys.S)
        {
            // Take a screenshot
            ScreenShotFactory.saveScreenshot();
        }
        else if (keycode == Input.Keys.B)
        {
            // Switch to build
            game.clickState = game.BUILD;
        }
        return true;
    }
    
    /**
     * Dispose of this menu
     */
    public void dispose()
    {
        
    }
    
    /**
     * Hide this menu
     */
    public void hide()
    {
        
    }
    
    /**
     * Pause this menu
     */
    public void pause()
    {
        
    }
    
    /**
     * Resume this menu
     */
    public void resume()
    {
        
    }
    
    /**
     * Resize this menu
     */
    public void resize(int width, int height)
    {
        
    }
    
}  // end PlayGame
