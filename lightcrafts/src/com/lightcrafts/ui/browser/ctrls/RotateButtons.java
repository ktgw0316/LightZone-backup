/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.ctrls;

import com.lightcrafts.ui.browser.view.AbstractImageBrowser;
import com.lightcrafts.ui.browser.view.ImageBrowserActions;
import static com.lightcrafts.ui.browser.ctrls.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;

import javax.swing.*;

/**
 * A Box holding two buttons, to rotate left and right in a browser.
 */
public class RotateButtons extends Box {

    private final static Icon LeftIcon = ButtonFactory.getIconByName("rotateLeft" );
    private final static Icon RightIcon = ButtonFactory.getIconByName("rotateRight" );
    private final static Icon HorizontalIcon = ButtonFactory.getIconByName("rotateLeft"); // TOCO:
    private final static Icon VerticalIcon = ButtonFactory.getIconByName("rotateRight"); // TOCO:

    private final static String LeftToolTip = LOCALE.get("RotateLeftToolTip");
    private final static String RightToolTip = LOCALE.get("RotateRightToolTip");
    private final static String HorizontalToolTip = LOCALE.get("FlipHorizontalToolTip");
    private final static String VerticalToolTip = LOCALE.get("FlipVerticalToolTip");

    public RotateButtons(AbstractImageBrowser browser) {
        super(BoxLayout.X_AXIS);

        final ImageBrowserActions actions = browser.getActions();

        final Action leftAction = actions.getLeftAction();
        final Action rightAction = actions.getRightAction();
        final Action horizontalAction = actions.getHorizontalAction();
        final Action verticalAction = actions.getVerticalAction();

        addButton(leftAction, LeftIcon, LeftToolTip);
        addButton(rightAction, RightIcon, RightToolTip);
        addButton(horizontalAction, HorizontalIcon, HorizontalToolTip);
        addButton(verticalAction, VerticalIcon, VerticalToolTip);
    }

    private void addButton(Action action, Icon icon, String toolTip) {
        final CoolButton button = new CoolButton();
        button.setAction(action);
        button.setIcon(icon);
        button.setToolTipText(toolTip);
        add(button);
    }
}
